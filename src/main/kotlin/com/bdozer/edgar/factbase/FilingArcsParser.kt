package com.bdozer.edgar.factbase

import com.bdozer.edgar.XbrlNamespaces.link
import com.bdozer.edgar.XbrlNamespaces.xlink
import com.bdozer.edgar.factbase.XLinkExtentions.from
import com.bdozer.edgar.factbase.XLinkExtentions.href
import com.bdozer.edgar.factbase.XLinkExtentions.label
import com.bdozer.edgar.factbase.XLinkExtentions.role
import com.bdozer.edgar.factbase.XLinkExtentions.to
import com.bdozer.edgar.factbase.XLinkExtentions.weight
import com.bdozer.edgar.factbase.dataclasses.Arc
import com.bdozer.edgar.factbase.dataclasses.Calculation
import com.bdozer.edgar.factbase.dataclasses.FilingArcs
import com.bdozer.edgar.factbase.ingestor.InstanceDocumentExtensions.documentFiscalPeriodFocus
import com.bdozer.edgar.factbase.ingestor.InstanceDocumentExtensions.documentFiscalYearFocus
import com.bdozer.edgar.factbase.ingestor.InstanceDocumentExtensions.documentPeriodEndDate
import com.bdozer.edgar.factbase.ingestor.InstanceDocumentExtensions.formType
import com.bdozer.xml.XmlNode
import java.net.URI
import java.util.*

class FilingArcsParser(private val filingProvider: FilingProvider) {

    private val incomeStatementRootHref = "us-gaap_IncomeStatementAbstract"
    private val balanceSheetRootHref = "us-gaap_StatementOfFinancialPositionAbstract"
    private val conceptManager = filingProvider.conceptManager()

    /**
     * Parse filing calculations
     */
    fun parseFilingArcs(): FilingArcs {
        val incomeStatement = traversePresentation(incomeStatementRootHref)
        val balanceSheet = traversePresentation(balanceSheetRootHref)

        val instanceDocument = filingProvider.instanceDocument()
        val cik = filingProvider.cik()
        val adsh = filingProvider.adsh()

        return FilingArcs(
            _id = "$cik:$adsh",
            cik = cik,
            adsh = adsh,
            formType = instanceDocument.formType(),
            documentFiscalPeriodFocus = instanceDocument.documentFiscalPeriodFocus(),
            documentPeriodEndDate = instanceDocument.documentPeriodEndDate()
                ?: error("documentPeriodEndDate not in document"),
            documentFiscalYearFocus = instanceDocument.documentFiscalYearFocus(),
            incomeStatement = incomeStatement,
            balanceSheet = balanceSheet,
        )
    }

    /**
     * Traverse presentation XMLs and construct [Arc]
     */
    private fun traversePresentation(rootLocator: String): List<Arc> {

        val presentation = filingProvider.presentationLinkbase()

        /*
        Find the presentationLink that contains the given root locator
         */
        val presentationLink = presentation
            .getElementsByTag(link, "presentationLink")
            .find { presentationLink ->
                /*
                The correct presentationLink to use is the one
                that contains our rootLocator and is not a parenthetical
                 */
                presentationLink
                    .getElementsByTag(link, "loc")
                    .any { loc ->
                        val href = loc.href() ?: error("...")
                        URI(href).fragment == rootLocator && !href.toLowerCase().endsWith("parenthetical")
                    }
            } ?: error("unable to find a presentation link with root locator $rootLocator")

        return traversePresentationLink(presentationLink, rootLocator)
    }

    private fun traversePresentationLink(
        presentationLink: XmlNode,
        rootLocator: String? = null
    ): MutableList<Arc> {
        /*
        For some reason this keeps happening
         */
        val role = presentationLink.role() ?: error("presentationLink has no role attribute")
        val arcs = presentationLink
            .getElementsByTag(link, "presentationArc")
            .groupBy { it.from() }
        val locators = presentationLink
            .getElementsByTag(link, "loc")

        val locatorHrefs = locators
            .associate {
                it.label() to it.href()
            }

        /*
        CalculationArcs is a lookup take for Calculations given the href
        of a single locator
         */
        val calculationArcs = parseCalculationArcs(role)

        if (locators.isEmpty()) {
            return mutableListOf()
        }
        val rootLocatorLabel = locators
            .find { loc -> loc.href()?.let { URI(it).fragment } == rootLocator }
            .label() ?: locators.first().label()

        val stack = Stack<String>()
        val parents = Stack<String>()
        stack.add(rootLocatorLabel)

        /*
        lastSiblings stack is used to trace the "level" of graph
        we are at, it records the last sibling among children each time an
        list of children is encountered. When the main stack is popped
        to a point such that the node being processed == the lastSibling in the
        lastSiblings stack we know we've finished process all the children
        for the current level

        therefore, when the Stack has been popped
         */
        val lastSiblings = Stack<String>()
        lastSiblings.add(rootLocatorLabel)

        /*
        Perform a DFS walk of the presentation arcs
        using xlink:from -> xlink:to, as we encounter new nodes
        we derive the corresponding concept's calculation shallowly
         */
        val graphNodes = mutableListOf<Arc>()
        while (stack.isNotEmpty()) {
            val currLocLabel = stack.pop()
            val conceptHref = locatorHrefs[currLocLabel] ?: error("cannot find $currLocLabel in locators")

            /*
            Add the graph node into it's appropriate place in the final
            data structure
             */
            graphNodes.add(
                Arc(
                    conceptHref = conceptHref,
                    calculations = calculationArcs[conceptHref] ?: emptyList(),
                    parentHref = if (parents.isNotEmpty()) locatorHrefs[parents.peek()] else null,
                    conceptName = conceptManager.getConcept(conceptHref)?.conceptName!!,
                )
            )

            /*
            We've processed all the children for the current parent
             */
            if (lastSiblings.isNotEmpty() && lastSiblings.peek() == currLocLabel) {
                lastSiblings.pop()
                if (parents.isNotEmpty()) {
                    parents.pop()
                }
            }

            val children = arcs[currLocLabel]
            if (!children.isNullOrEmpty()) {
                val elements = children.map { it.attr(xlink, "to") }.reversed()
                stack.addAll(elements)
                val lastSibling = elements.first()
                parents.add(currLocLabel)
                lastSiblings.add(lastSibling)
            }
        }

        return graphNodes
    }

    /**
     * Combs through `<calculationArc />` instances, and extracting the relationships
     * between concepts and their calculations
     */
    private fun parseCalculationArcs(preferredRole: String): Map<String, List<Calculation>> {
        val calculationLinkbase = filingProvider.calculationLinkbase()

        /**
         * Nested function to help turn a specific calculationLink
         * into a lookup map, we will perform this operation on all
         * calculation link
         */
        fun parseCalculationLink(calculationLink: XmlNode?): Map<String, List<Calculation>> {
            val calculationLocs = calculationLink
                ?.getElementsByTag(link, "loc")
                ?.associate { it.label() to it.href() }
                ?: emptyMap()
            return calculationLink
                ?.getElementsByTag(link, "calculationArc")
                ?.groupBy { it.from() }
                ?.entries
                ?.associate { (from, nodes) ->
                    val calculations = nodes.map { node ->
                        val conceptHref = calculationLocs[node.to()] ?: error("...")
                        Calculation(
                            conceptHref = conceptHref,
                            weight = node.weight(),
                            conceptName = conceptManager.getConcept(conceptHref)?.conceptName
                                ?: error("cannot find concept for $conceptHref")
                        )
                    }
                    val fromConceptHref = calculationLocs[from] ?: error("...")
                    fromConceptHref to calculations
                } ?: emptyMap()
        }

        val calculationLinks = calculationLinkbase.getElementsByTag(link, "calculationLink")
        /*
        a map of role -> conceptHref -> Calculation
         */
        val allLookups = calculationLinks
            .filter { it.role() != null }
            .associate { calculationLink ->
                calculationLink.role()!! to parseCalculationLink(calculationLink)
            }

        /*
        we flatten the map above for every conceptHref, giving
        preferential treatment to the default calculationLink that match
        the passed in role
         */
        val toMap = allLookups
            .flatMap { (role, lookup) ->
                lookup.map { (conceptHref, _) ->
                    conceptHref to role
                }
            }
            .groupBy { it.first }
            .map { (conceptHref, roles) ->
                val role = (roles.find { it.second == preferredRole } ?: roles.first())?.second
                conceptHref to allLookups[role]?.get(conceptHref)!!
            }
            .toMap()
        return toMap
    }
}