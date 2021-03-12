package com.starburst.starburst.edgar.factbase.modelbuilder.skeletongenerator

import com.starburst.starburst.edgar.XbrlConstants.link
import com.starburst.starburst.edgar.XbrlConstants.xlink
import com.starburst.starburst.edgar.dataclasses.ElementDefinition
import com.starburst.starburst.edgar.dataclasses.Fact
import com.starburst.starburst.edgar.dataclasses.XbrlExplicitMember
import com.starburst.starburst.edgar.factbase.modelbuilder.DependencyFlattener
import com.starburst.starburst.edgar.factbase.modelbuilder.skeletongenerator.HistoricalValueExtensions.allHistoricalValues
import com.starburst.starburst.edgar.factbase.modelbuilder.skeletongenerator.HistoricalValueExtensions.latestHistoricalValue
import com.starburst.starburst.edgar.factbase.modelbuilder.skeletongenerator.RoleRefsExtensions.findBalanceSheetRole
import com.starburst.starburst.edgar.factbase.modelbuilder.skeletongenerator.RoleRefsExtensions.findCashFlowStatementRole
import com.starburst.starburst.edgar.factbase.modelbuilder.skeletongenerator.RoleRefsExtensions.findIncomeStatementRole
import com.starburst.starburst.edgar.factbase.support.SchemaManager
import com.starburst.starburst.edgar.provider.FilingProvider
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.xml.XmlElement
import com.starburst.starburst.xml.XmlNode
import org.slf4j.LoggerFactory

/**
 * [SkeletonGenerator] performs the heavily lifting of
 * parsing XMLs (most of it done in init {} block)
 */
class SkeletonGenerator(
    val filingProvider: FilingProvider,
    val schemaManager: SchemaManager,
    val facts: List<Fact>,
) {

    private val log = LoggerFactory.getLogger(SkeletonGenerator::class.java)

    val model: Model

    /**
     * A lookup map from itemName (XML tag name) -> the XML [ElementDefinition] declared in either the
     * GAAP or extension XSD, this faciliates lookup of an item to it's definition in the XSDs
     * but in a type-safe manner
     */
    val elementDefinitionMap = mutableMapOf<String, ElementDefinition>()

    /**
     * This is a immediate item dependency graph, without flattening out
     * transitive dependencies. This is directly populated from calculationArc(s)
     * from the Edgar XBRL calculation link base XML files. To see transitive dependencies
     * flattend out see [flattenedItemDependencyGraph]
     */
    val itemDependencyGraph = mutableMapOf<String, List<String>>()

    /**
     * This is a item dependency graph except flattened out
     * via DFS algorithm
     *
     * Thus, if `Net Income = Revenue - Operating Expenses`
     * and `Operating Expenses = R&D + Marketing`
     * then in this graph, look up for Net Income will result in
     * `["Revenue", "R&D", "Marketing"]`
     */
    val flattenedItemDependencyGraph: Map<String, Set<String>>

    /**
     * [items] hold a mapping between locator label -> [Item]
     * this allow the context to tract every item that has been created
     * and ensure their creation and mutation are not duplicated
     */
    val items: Map<String?, Item>
    val incomeStatementCalculationItems: List<Item>
    val balanceSheetCalculationItems: List<Item>
    val cashFlowStatementItems: List<Item>

    private fun XmlNode.label() = this.attr(xlink, "label")
    private fun XmlNode.href() = this.attr(xlink, "href")
    private val calculationLinkbase: XmlElement = filingProvider.calculationLinkbase()

    private val incomeStatementCalculationLink: XmlNode
    private val balanceSheetCalculationLink: XmlNode
    private val cashFlowStatementCalculationLink: XmlNode

    /**
     * [effectiveLocatorLookup] is a mapping of locator labels to other locator labels
     * when duplicate locators are found, this mapping enable one to traverse
     * from the duplicate to the meaningful locator
     */

    private val effectiveLocatorLookup: Map<String?, String?>

    /**
     * [originalLocatorLookup] is a mapping of locator labels to their loc [XmlNode]
     * as it was declared in the calculation linkbase XML
     */
    private val originalLocatorLookup: Map<String?, XmlNode?>

    /**
     * [calculationArcs] is a mapping of locator labels to 'child'
     * labels as defined by xlink:to XML attr in any calculation arcs
     */
    private val calculationArcs: Map<String?, List<XmlNode>>

    /**
     * Most of the heavy lifting of parsing calculation link base XML
     * is actually done in here in the initialization of the helper
     * as this is the point at which we collate and clean, map all the locator and arc data
     */
    init {
        log.info("Initializing ${SkeletonGenerator::class.java.simpleName}")

        /*
        start with the calculation XML since
        from there we derive the structure of the `Model` to be created
        everything else serves as a reference (including "XBRL facts" such as actual historical values)
        to the business logic expressed within this page
         */
        val allCalculationLinks = calculationLinkbase.getElementsByTag(link, "calculationLink")

        /*
        find the income statement calculation links
         */
        val incomeStatementRole = calculationLinkbase
            .getElementsByTag(link, "roleRef").findIncomeStatementRole()
        incomeStatementCalculationLink = findLinkCalculationByRole(allCalculationLinks, incomeStatementRole)

        /*
        find the balance sheet calculation links
         */
        val balanceSheetRole = calculationLinkbase
            .getElementsByTag(link, "roleRef")
            .findBalanceSheetRole()
        balanceSheetCalculationLink = findLinkCalculationByRole(allCalculationLinks, balanceSheetRole)

        /*
        find the cash flow statement calculation links
         */
        val cashFlowStatementRole = calculationLinkbase
            .getElementsByTag(link, "roleRef")
            .findCashFlowStatementRole()
        cashFlowStatementCalculationLink = findLinkCalculationByRole(allCalculationLinks, cashFlowStatementRole)

        /*
        grab all the locators, find the duplicates
        and then for each locator find all of it's associated calculationArc(s)
         */
        val allElements =
            listOf(incomeStatementCalculationLink, balanceSheetCalculationLink, cashFlowStatementCalculationLink)

        calculationArcs = allElements
            .flatMap { calculationLinks ->
                calculationLinks.getElementsByTag(link, "calculationArc")
            }
            .groupBy { it.attr(xlink, "from") }


        /*
         create a reference map to allow subsequent iteration through the calculationArcs
         to remap the locator labels

         so if locators 'a' and 'b' both href 'foo.xsd#GrossProfit', and 'a' is blank while 'b' has 2 calculationArcs
         then the resulting map should be
         'a' -> 'b'
         'b' -> 'b'
         */
        val locators = allElements
            .flatMap { calculationLinks ->
                calculationLinks.getElementsByTag(link, "loc")
            }

        effectiveLocatorLookup = locatorsToRefs(locators)
        originalLocatorLookup = locators.associateBy { it.label() }
        items = effectiveLocatorLookup
            .values
            .map { locLabel ->
                val locator = originalLocatorLookup[locLabel] ?: error("...")
                locLabel to createItem(locator)
            }
            .toMap()

        incomeStatementCalculationItems = incomeStatementCalculationLink.toItems()
        balanceSheetCalculationItems = balanceSheetCalculationLink.toItems()
        cashFlowStatementItems = cashFlowStatementCalculationLink.toItems()

        /*
        as we encounter "location"s we create placeholder for them as Item(s)
        their historical values are resolved via look up against the Instance document
        labels are resolved via the label document
         */
        val name = entityRegistrantName()
        val symbol = tradingSymbol()
        val cik = filingProvider.cik()

        model = Model(
            name = name,
            symbol = symbol,
            cik = cik,
            incomeStatementItems = incomeStatementCalculationItems,
            balanceSheetItems = balanceSheetCalculationItems,
            cashFlowStatementItems = cashFlowStatementItems,
        )

        /*
        Create a flattened out dependency graph as well
        this graph would flow from aggregate item to a list of items
        dependent up on it TODO this will be used to determine if an item flows into OperatingExpense, Revenue etc.
         */
        flattenedItemDependencyGraph = DependencyFlattener(itemDependencyGraph).flatten()

        log.info(
            "Finished initializing " +
                    "symbol=$symbol, " +
                    "name=$name, " +
                    "cik=$cik, " +
                    "${SkeletonGenerator::class.java.simpleName}, " +
                    "calculationArcs=${calculationArcs.size}, " +
                    "locatorRefs=${effectiveLocatorLookup.size}, " +
                    "incomeStatementRole=$incomeStatementRole, " +
                    "balanceSheetRole=$balanceSheetRole, " +
                    "cashFlowStatementRole=$cashFlowStatementRole"
        )
    }

    private fun XmlNode.toItems(): List<Item> {
        return this
            .getElementsByTag(link, "loc")
            .map { locator ->
                val originalLabel = locator.label()
                val effectiveLabel = effectiveLocatorLookup[originalLabel]
                val item = items[effectiveLabel]
                item ?: error("unable to find item for originalLabel=$originalLabel effectiveLoc=$effectiveLabel")
            }
            .distinct()
    }

    private fun locatorsToRefs(locators: List<XmlNode>): Map<String?, String?> {
        return locators
            .groupBy { it.href() }
            .entries
            .flatMap { (_, locators) ->
                /*
                if there are multiple locators, figure out which node has
                associated calculationArcs defining it on "xlink:from"
                 */
                val firstNodeLabel = locators.first().label()
                if (locators.size > 1) {
                    // find the first node that have associated calculationArcs
                    val firstCalculatedNode = locators.find { node ->
                        val label = node.label()
                        calculationArcs[label]?.isNotEmpty() == true
                    } ?: locators.first()

                    locators.map { node ->
                        val fromLabel = node.label()
                        val toLabel = firstCalculatedNode.label()
                        fromLabel to toLabel
                    }
                } else {
                    listOf(firstNodeLabel to firstNodeLabel)
                }
            }
            .toMap()
    }

    /**
     * This method creates an [Item] from a locator
     * this method does not check if an [Item] has already been created
     * see [getItemForLocator]
     */
    private fun createItem(loc: XmlNode): Item {
        val elementDefinition = retrieveElementDefinition(loc)

        /*
        the fragment is actually the id to look up by
         */
        val itemName = elementDefinition.name

        /*
        populate the historical value of the item
         */
        val latestHistoricalFact = latestFact(itemName)
        val historicalValues = allHistoricalValues(
            elementName = itemName,
            explicitMembers = latestHistoricalFact?.explicitMembers ?: emptyList()
        )
        val historicalValue = latestHistoricalValue(itemName)?.value ?: 0.0

        fun getItemNameFromNode(node: XmlNode): String {
            val to = node.attr(xlink, "to")
            val toHref = originalLocatorLookup[to]
                ?.href()
                ?: error("cannot find loc for $to")
            /*
            the fragment is actually the id to look up by
             */
            return (schemaManager.getElementDefinition(toHref)
                ?: error("unable to find a schema definition for $toHref")).name
        }

        /*
        it is important that our locLabel traverses
        the reference to avoid duplicates
         */
        val locLabel = effectiveLocatorLookup[loc.label()]
        val relatedCalcArcs = calculationArcs[locLabel]
        val dependentItems = relatedCalcArcs?.map { node -> getItemNameFromNode(node) } ?: emptyList()

        /*
        after figuring out dependent items from calculationArcs, store this dependency
        in the context's graph for downstream processor
        */
        itemDependencyGraph[itemName] = dependentItems

        // if there are no calculation arcs
        // leave the amount to be the most recent reported number
        val expression = if (relatedCalcArcs == null) {
            "$historicalValue"
        } else {
            val positives = relatedCalcArcs.filter { node -> node.attr("weight")?.toDouble() == 1.0 }
                .joinToString("+") { node -> getItemNameFromNode(node) }
            val negatives = relatedCalcArcs.filter { node -> node.attr("weight")?.toDouble() == -1.0 }
                .joinToString("-") { node -> getItemNameFromNode(node) }
            when {
                negatives.isBlank() -> {
                    positives
                }
                positives.isBlank() -> {
                    negatives
                }
                else -> {
                    "$positives-$negatives"
                }
            }
        }

        return Item(
            name = itemName,
            description = latestHistoricalFact?.labelTerse,
            historicalValue = historicalValue,
            historicalValues = historicalValues,
            expression = expression
        )
    }

    private fun retrieveElementDefinition(loc: XmlNode): ElementDefinition {
        val locHref = loc.href()
        val elementDefinition = locHref?.let { href ->
            schemaManager.getElementDefinition(href)
        } ?: error("Unable to find element definition name for $locHref")
        elementDefinitionMap[elementDefinition.name] = elementDefinition
        return elementDefinition
    }

    fun entityRegistrantName(): String {
        return facts
            .filter { it.elementName == "EntityRegistrantName" && it.explicitMembers.isEmpty() }
            .maxByOrNull { it.documentPeriodEndDate }
            ?.stringValue ?: ""
    }

    fun tradingSymbol(): String {
        return facts
            .filter { it.elementName == "TradingSymbol" && it.explicitMembers.isEmpty() }
            .maxByOrNull { it.documentPeriodEndDate }
            ?.stringValue ?: ""
    }

    private fun latestFact(elementName: String, explicitMembers: List<XbrlExplicitMember> = emptyList()): Fact? {
        return facts
            .filter { it.elementName == elementName && it.explicitMembers == explicitMembers }
            .maxByOrNull { it.documentPeriodEndDate }
    }

    private fun findLinkCalculationByRole(calculationLinks: List<XmlNode>, role: String): XmlNode {
        return calculationLinks
            .find { it.attr(xlink, "role") == role }
            ?: error("cannot find $role")
    }

}
