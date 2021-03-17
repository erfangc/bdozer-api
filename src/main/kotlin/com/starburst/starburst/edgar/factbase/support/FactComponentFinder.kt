package com.starburst.starburst.edgar.factbase.support

import com.mongodb.client.MongoDatabase
import com.starburst.starburst.edgar.XbrlNamespaces.link
import com.starburst.starburst.edgar.XbrlNamespaces.xlink
import com.starburst.starburst.edgar.explorer.EdgarExplorer
import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.dataclasses.FactComponentsResponse
import com.starburst.starburst.edgar.factbase.modelbuilder.skeletongenerator.RoleRefsExtensions.findIncomeStatementRole
import com.starburst.starburst.edgar.provider.FilingProvider
import com.starburst.starburst.edgar.provider.FilingProviderFactory
import com.starburst.starburst.xml.XmlNode
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.springframework.stereotype.Service
import java.net.URI
import java.util.*

/**
 * [FactComponentFinder] takes a concept defined by a filer and
 * find it's components (if any) in the EDGAR based fact base
 * and return all of the historical data associated with those components
 *
 * Ex:
 * - OperatingExpense -> SG&A, Selling and Marketing + Restructuring
 */
@Service
class FactComponentFinder(
    mongoDatabase: MongoDatabase,
    private val filingProviderFactory: FilingProviderFactory,
    private val edgarExplorer: EdgarExplorer,
) {

    internal data class GraphNode(val conceptHref: String, val componentHrefs: List<String>)

    private val root = "http://xbrl.fasb.org/us-gaap/2020/elts/us-gaap-2020-01-31.xsd#us-gaap_IncomeStatementAbstract"
    private val col = mongoDatabase.getCollection<Fact>()

    fun factComponents(cik: String, conceptId: String): FactComponentsResponse {

        val adsh = adsh(cik)
        val filingProvider = filingProviderFactory.createFilingProvider(cik, adsh)
        val graphNodes = traversePresentation(filingProvider)

        val components = graphNodes
            .find { URI(it.conceptHref).fragment == conceptId }
            ?.componentHrefs
            ?.map { conceptHref -> conceptHref }
            ?.toSet() ?: emptySet()

        val facts = col
            .find(Fact::cik eq cik)
            .filter { fact -> components.contains(fact.conceptHref) }

        return FactComponentsResponse(
            facts = facts,
            latestAnnualFacts = latestAnnualFacts(facts),
            latestQuarterFacts = latestQuarterFacts(facts)
        )
    }

    private fun latestQuarterFacts(facts: List<Fact>): List<Fact> {
        return facts
            .filter { it.formType == "10-Q" }
            .groupBy { it.documentPeriodEndDate }
            .maxByOrNull { it.key }?.value ?: emptyList()
    }

    private fun latestAnnualFacts(facts: List<Fact>): List<Fact> {
        return facts
            .filter { it.formType == "10-K" }
            .groupBy { it.documentPeriodEndDate }
            .maxByOrNull { it.key }?.value ?: emptyList()
    }

    private fun adsh(cik: String) = edgarExplorer
        .searchFilings(cik)
        .sortedByDescending { it.period_ending }
        .find { it.form == "10-K" }
        ?.adsh ?: error("no 10-K filings found for $cik")

    /**
     * Traverse presentation XMLs and construct [GraphNode]
     */
    private fun traversePresentation(filingProvider: FilingProvider): List<GraphNode> {

        val presentation = filingProvider.presentationLinkbase()
        val graphNodes = mutableListOf<GraphNode>()

        val role = presentation
            .getElementsByTag(link, "roleRef")
            .findIncomeStatementRole()
        val presentationLink = presentation
            .getElementsByTag(link, "presentationLink")
            .find { it.role() == role }!!
        val arcs = presentationLink
            .getElementsByTag(link, "presentationArc")
            .groupBy { it.from() }
        val locs = presentationLink
            .getElementsByTag(link, "loc")

        val locatorHrefs = locs
            .associate {
                it.label() to it.href()
            }

        val calculationArcs = parseCalculationArcs(filingProvider)

        val rootLoc = locs
            .find { it.href() == root }
            .label()

        val stack = Stack<String>()
        stack.add(rootLoc)

        /*
        this stack is used to trace the "level" of graph
        we are at, it records the last sibling each time an
        list of children is encountered

        therefore, when the Stack has been popped
         */
        val lastSibling = Stack<String>()
        lastSibling.add(rootLoc)

        /*
        perform a DFS walk of the presentation arcs
        using xlink:from -> xlink:to, as we encounter new nodes
        we derive the corresponding concept's calculation shallowly
         */
        while (stack.isNotEmpty()) {
            val node = stack.pop()
            val conceptName = locatorHrefs[node] ?: error("...")
            val listComponents = calculationArcs[conceptName]?.filterNotNull() ?: emptyList()
            graphNodes.add(GraphNode(conceptHref = conceptName, componentHrefs = listComponents))
            if (lastSibling.peek() == node) {
                lastSibling.pop()
            }
            val children = arcs[node]
            if (!children.isNullOrEmpty()) {
                val elements = children.map { it.attr(xlink, "to") }.reversed()
                stack.addAll(elements)
                lastSibling.add(elements.first())
            }
        }

        return graphNodes
    }

    private fun parseCalculationArcs(filingProvider: FilingProvider): Map<String?, List<String?>> {
        val cal = filingProvider.calculationLinkbase()
        val role = cal.getElementsByTag(link, "roleRef").findIncomeStatementRole()
        val calculationLink = cal
            .getElementsByTag(link, "calculationLink")
            .find { it.role() == role }!!

        val calculationLocs = calculationLink
            .getElementsByTag(link, "loc")
            .associate { it.label() to it.href() }

        return calculationLink
            .getElementsByTag(link, "calculationArc")
            .groupBy { it.from() }
            .entries
            .associate { (from, nodes) ->
                calculationLocs[from] to nodes.map { calculationLocs[it.to()] }
            }
    }

    private fun XmlNode?.label(): String? {
        return this?.attr(xlink, "label")
    }

    private fun XmlNode?.href(): String? {
        return this?.attr(xlink, "href")
    }

    private fun XmlNode?.role(): String? {
        return this?.attr(xlink, "role")
    }

    private fun XmlNode?.from(): String? {
        return this?.attr(xlink, "from")
    }

    private fun XmlNode?.to(): String? {
        return this?.attr(xlink, "to")
    }

}