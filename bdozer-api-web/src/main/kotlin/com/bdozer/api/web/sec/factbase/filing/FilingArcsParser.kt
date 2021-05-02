package com.bdozer.api.web.sec.factbase.filing

import com.bdozer.api.web.sec.XbrlNamespaces.link
import com.bdozer.api.web.sec.XbrlNamespaces.xlink
import com.bdozer.api.web.sec.factbase.XLinkExtentions.from
import com.bdozer.api.web.sec.factbase.XLinkExtentions.href
import com.bdozer.api.web.sec.factbase.XLinkExtentions.label
import com.bdozer.api.web.sec.factbase.XLinkExtentions.role
import com.bdozer.api.web.sec.factbase.XLinkExtentions.to
import com.bdozer.api.web.sec.factbase.XLinkExtentions.weight
import com.bdozer.api.common.dataclasses.sec.Arc
import com.bdozer.api.common.dataclasses.sec.Calculation
import com.bdozer.api.common.dataclasses.sec.FilingArcs
import com.bdozer.api.web.sec.factbase.ingestor.InstanceDocumentExtensions.documentFiscalPeriodFocus
import com.bdozer.api.web.sec.factbase.ingestor.InstanceDocumentExtensions.documentFiscalYearFocus
import com.bdozer.api.web.sec.factbase.ingestor.InstanceDocumentExtensions.documentPeriodEndDate
import com.bdozer.api.web.sec.factbase.ingestor.InstanceDocumentExtensions.formType
import com.bdozer.api.web.xml.XmlNode
import java.net.URI
import java.util.*

class FilingArcsParser(private val secFiling: SECFiling) {

    private val incomeStatementRootHref = "us-gaap_IncomeStatementAbstract"
    private val balanceSheetRootHref = "us-gaap_StatementOfFinancialPositionAbstract"
    private val conceptManager = secFiling.conceptManager

    /**
     * Parse filing calculations
     */
    fun parseFilingArcs(): FilingArcs {
        val incomeStatement = traversePresentation(incomeStatementRootHref)
        val balanceSheet = traversePresentation(balanceSheetRootHref)

        val instanceDocument = secFiling.instanceDocument
        val cik = secFiling.cik
        val adsh = secFiling.adsh

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

        val presentation = secFiling.presentationLinkbase

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
    ): List<Arc> {
        /*
        For some reason this keeps happening
         */
        val role = presentationLink.role() ?: error("presentationLink has no role attribute")
        val xmlNodes = presentationLink
            .getElementsByTag(link, "presentationArc")
            .groupBy { it.from() }
        val locators = presentationLink
            .getElementsByTag(link, "loc")

        val hrefLookupByLabel = locators
            .associate {
                it.label() to it.href()
            }
        val locatorByHref = locators
            .associateBy { it.href() }

        /*
        CalculationArcs is a lookup take for Calculations given the href
        of a single locator
         */
        val calculationsLookup = parseCalculationArcs(role)

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
        val arcs = mutableListOf<Arc>()
        while (stack.isNotEmpty()) {
            val currentLocLabel = stack.pop()
            val conceptHref = hrefLookupByLabel[currentLocLabel] ?: error("cannot find $currentLocLabel in locators")

            /*
            Add the graph node into it's appropriate place in the final
            data structure
             */
            val calculations = calculationsLookup[conceptHref] ?: emptyList()
            /*
            if conceptHrefs referenced by calculations do not exist
            then remove them
             */
            arcs.add(
                Arc(
                    conceptHref = conceptHref,
                    calculations = if (calculations.any { calculation -> locatorByHref[calculation.conceptHref] == null }) emptyList() else calculations,
                    parentHref = if (parents.isNotEmpty()) hrefLookupByLabel[parents.peek()] else null,
                    conceptName = conceptManager.getConcept(conceptHref)?.conceptName
                        ?: error("concept $conceptHref not found"),
                )
            )

            /*
            We've processed all the children for the current parent
             */
            if (lastSiblings.isNotEmpty() && lastSiblings.peek() == currentLocLabel) {
                lastSiblings.pop()
                if (parents.isNotEmpty()) {
                    parents.pop()
                }
            }

            val children = xmlNodes[currentLocLabel]
            if (!children.isNullOrEmpty()) {
                val elements = children.map { it.attr(xlink, "to") }.reversed()
                stack.addAll(elements)
                val lastSibling = elements.first()
                parents.add(currentLocLabel)
                lastSiblings.add(lastSibling)
            }
        }

        return arcs
    }

    /**
     * Combs through `<calculationArc />` instances, and extracting the relationships
     * between concepts and their calculations
     */
    private fun parseCalculationArcs(preferredRole: String): Map<String, List<Calculation>> {
        val calculationLinkbase = secFiling.calculationLinkbase

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
        return allLookups
            .flatMap { (role, lookup) ->
                lookup.map { (conceptHref, _) ->
                    conceptHref to role
                }
            }
            .groupBy { it.first }
            .map { (conceptHref, roles) ->
                val role = (roles.find { it.second == preferredRole } ?: roles.first()).second
                conceptHref to allLookups[role]?.get(conceptHref)!!
            }
            .toMap()
    }
}