package com.starburst.starburst.edgar.factbase.ingestor

import com.starburst.starburst.edgar.FilingProvider
import com.starburst.starburst.edgar.XbrlNamespaces
import com.starburst.starburst.edgar.XbrlNamespaces.link
import com.starburst.starburst.edgar.factbase.XLinkExtentions.from
import com.starburst.starburst.edgar.factbase.XLinkExtentions.href
import com.starburst.starburst.edgar.factbase.XLinkExtentions.label
import com.starburst.starburst.edgar.factbase.XLinkExtentions.role
import com.starburst.starburst.edgar.factbase.XLinkExtentions.to
import com.starburst.starburst.edgar.factbase.XLinkExtentions.weight
import com.starburst.starburst.edgar.factbase.ingestor.InstanceDocumentExtensions.documentFiscalPeriodFocus
import com.starburst.starburst.edgar.factbase.ingestor.InstanceDocumentExtensions.documentFiscalYearFocus
import com.starburst.starburst.edgar.factbase.ingestor.InstanceDocumentExtensions.documentPeriodEndDate
import com.starburst.starburst.edgar.factbase.ingestor.InstanceDocumentExtensions.formType
import com.starburst.starburst.edgar.factbase.ingestor.dataclasses.Calculation
import com.starburst.starburst.edgar.factbase.ingestor.dataclasses.FilingCalculations
import com.starburst.starburst.edgar.factbase.ingestor.dataclasses.SectionNode
import com.starburst.starburst.edgar.factbase.modelbuilder.skeletongenerator.RoleRefsExtensions.findBalanceSheetRole
import com.starburst.starburst.edgar.factbase.modelbuilder.skeletongenerator.RoleRefsExtensions.findCashFlowStatementRole
import com.starburst.starburst.edgar.factbase.modelbuilder.skeletongenerator.RoleRefsExtensions.findIncomeStatementRole
import java.util.*

class CalculationsParser(private val filingProvider: FilingProvider) {

    private val incomeStatementRootHref =
        "http://xbrl.fasb.org/us-gaap/2020/elts/us-gaap-2020-01-31.xsd#us-gaap_IncomeStatementAbstract"

    private val cashFlowStatementRootHref =
        "http://xbrl.fasb.org/us-gaap/2020/elts/us-gaap-2020-01-31.xsd#us-gaap_StatementOfCashFlowsAbstract"

    private val balanceSheetRootHref =
        "http://xbrl.fasb.org/us-gaap/2020/elts/us-gaap-2020-01-31.xsd#us-gaap_StatementOfFinancialPositionAbstract"

    fun parseCalculations(): FilingCalculations {
        val presentationLinkbase = filingProvider.presentationLinkbase()
        val roleRefs = presentationLinkbase.getElementsByTag(link, "roleRef")

        val incomeStatementRole = roleRefs.findIncomeStatementRole()
        val incomeStatementNodes = traversePresentation(incomeStatementRootHref, incomeStatementRole)

        val cashFlowStatementRole = roleRefs.findCashFlowStatementRole()
        val cashFlowStatementNodes = traversePresentation(cashFlowStatementRootHref, cashFlowStatementRole)

        val balanceSheetRole = roleRefs.findBalanceSheetRole()
        val balanceSheetNodes = traversePresentation(balanceSheetRootHref, balanceSheetRole)

        val instanceDocument = filingProvider.instanceDocument()
        return FilingCalculations(
            cik = filingProvider.cik(),
            adsh = filingProvider.adsh(),
            formType = instanceDocument.formType(),
            documentFiscalPeriodFocus = instanceDocument.documentFiscalPeriodFocus(),
            documentPeriodEndDate = instanceDocument.documentPeriodEndDate() ?: error("..."),
            documentFiscalYearFocus = instanceDocument.documentFiscalYearFocus(),
            incomeStatement = incomeStatementNodes,
            balanceSheet = balanceSheetNodes,
            cashFlowStatement = cashFlowStatementNodes
        )
    }

    /**
     * Traverse presentation XMLs and construct [SectionNode]
     */
    private fun traversePresentation(rootLocator: String, role: String): List<SectionNode> {

        val presentation = filingProvider.presentationLinkbase()
        val graphNodes = mutableListOf<SectionNode>()

        val presentationLink = presentation
            .getElementsByTag(link, "presentationLink")
            .find { it.role() == role }!!
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
        calculationArcs is a lookup take for Calculations given the href
        of a single locator
         */
        val calculationArcs = parseCalculationArcs(role)

        val rootLocatorLabel = locators
            .find { it.href() == rootLocator }
            .label()

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
        perform a DFS walk of the presentation arcs
        using xlink:from -> xlink:to, as we encounter new nodes
        we derive the corresponding concept's calculation shallowly
         */
        while (stack.isNotEmpty()) {
            val node = stack.pop()
            val conceptHref = locatorHrefs[node] ?: error("...")

            /*
            Add the graph node into it's appropriate place in the final
            data structure
             */
            graphNodes.add(
                SectionNode(
                    conceptHref = conceptHref,
                    calculations = calculationArcs[conceptHref] ?: emptyList(),
                    parentHref = if (parents.isNotEmpty()) locatorHrefs[parents.peek()] else null
                )
            )

            /*
            We've processed all the children for the current parent
             */
            if (lastSiblings.peek() == node) {
                lastSiblings.pop()
                if (parents.isNotEmpty()) {
                    parents.pop()
                }
            }

            val children = arcs[node]
            if (!children.isNullOrEmpty()) {
                val elements = children.map { it.attr(XbrlNamespaces.xlink, "to") }.reversed()
                stack.addAll(elements)
                val lastSibling = elements.first()
                parents.add(node)
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
                    Calculation(conceptHref = conceptHref, weight = node.weight())
                }
                val fromConceptHref = calculationLocs[from] ?: error("...")
                fromConceptHref to calculations
            } ?: emptyMap()
    }
}