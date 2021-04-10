package com.starburst.starburst.edgar.factbase.ingestor.support

import com.starburst.starburst.edgar.FilingProvider
import com.starburst.starburst.edgar.XbrlNamespaces.link
import com.starburst.starburst.edgar.XbrlNamespaces.xlink
import com.starburst.starburst.edgar.factbase.XLinkExtentions.from
import com.starburst.starburst.edgar.factbase.XLinkExtentions.href
import com.starburst.starburst.edgar.factbase.XLinkExtentions.label
import com.starburst.starburst.edgar.factbase.XLinkExtentions.role
import com.starburst.starburst.edgar.factbase.XLinkExtentions.to
import com.starburst.starburst.edgar.factbase.XLinkExtentions.weight
import com.starburst.starburst.edgar.factbase.dataclasses.Calculation
import com.starburst.starburst.edgar.factbase.dataclasses.FilingCalculations
import com.starburst.starburst.edgar.factbase.ingestor.InstanceDocumentExtensions.documentFiscalPeriodFocus
import com.starburst.starburst.edgar.factbase.ingestor.InstanceDocumentExtensions.documentFiscalYearFocus
import com.starburst.starburst.edgar.factbase.ingestor.InstanceDocumentExtensions.documentPeriodEndDate
import com.starburst.starburst.edgar.factbase.ingestor.InstanceDocumentExtensions.formType
import com.starburst.starburst.edgar.factbase.ingestor.dataclasses.Arc
import com.starburst.starburst.edgar.factbase.support.FilingConceptsHolder
import com.starburst.starburst.xml.XmlNode
import java.net.URI
import java.util.*

class FilingCalculationsParser(private val filingProvider: FilingProvider) {

    private val incomeStatementRootHref = "us-gaap_IncomeStatementAbstract"
    private val cashFlowStatementRootHref = "us-gaap_StatementOfCashFlowsAbstract"
    private val balanceSheetRootHref = "us-gaap_StatementOfFinancialPositionAbstract"
    private val conceptManager = FilingConceptsHolder(filingProvider = filingProvider)

    /**
     * Parse filing calculations
     */
    fun parseCalculations(): FilingCalculations {
        val incomeStatement = traversePresentation(incomeStatementRootHref)
        val cashFlowStatement = traversePresentation(cashFlowStatementRootHref)
        val balanceSheet = traversePresentation(balanceSheetRootHref)

        val instanceDocument = filingProvider.instanceDocument()
        val cik = filingProvider.cik()
        val adsh = filingProvider.adsh()

        return FilingCalculations(
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
            cashFlowStatement = cashFlowStatement
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
            if (lastSiblings.peek() == currLocLabel) {
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
    private fun parseCalculationArcs(role: String): Map<String, List<Calculation>> {
        val cal = filingProvider.calculationLinkbase()
        val calculationLink = cal
            .getElementsByTag(link, "calculationLink")
            .find { it.role() == role }

        val calculationLocs = calculationLink
            ?.getElementsByTag(link, "loc")
            ?.associate { it.label() to it.href() } ?: emptyMap()

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
                        conceptName = conceptManager.getConcept(conceptHref)?.conceptName!!
                    )
                }
                val fromConceptHref = calculationLocs[from] ?: error("...")
                fromConceptHref to calculations
            } ?: emptyMap()
    }
}