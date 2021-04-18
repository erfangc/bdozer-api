package com.bdozer.edgar.factbase.itemgenerator

import com.bdozer.edgar.dataclasses.Labels
import com.bdozer.edgar.dataclasses.XbrlExplicitMember
import com.bdozer.edgar.factbase.FactExtensions.filterForDimension
import com.bdozer.edgar.factbase.FilingProvider
import com.bdozer.edgar.factbase.dataclasses.Arc
import com.bdozer.edgar.factbase.dataclasses.Dimension
import com.bdozer.edgar.factbase.dataclasses.Fact
import com.bdozer.extensions.DoubleExtensions.orZero
import com.bdozer.models.dataclasses.*

/**
 * # Overview
 * [ItemGenerator] is responsible for turning a single presentation Arc
 * into one or more [Item] instances. A presentation arc typically represents a single concept.
 *
 * ## Simple translation
 *
 * In the absence of dimension declarations, the Arc could be thought of as having a soft 1-to-1 relationship with an [Item]
 * The generated item is backed by a dimensionless [Fact]
 *
 * ## When dimensions are declared
 *
 * When an arc defines a concept that must be decomposed into declared dimensions - then the arc
 * is translated into multiple [Item] each backed by their own [Fact]
 *
 * ## What else must this class do
 *
 *  - Create a consistent item naming convention so that items can be reliable and reproducibly generated
 *  - Generated [Item] instances with calculations defined must have those calculations populated
 *
 */
class ItemGenerator(private val filingProvider: FilingProvider) {

    /*
    Declare helpers
     */
    private val itemNameGenerator = ItemNameGenerator()
    private val filingArcsParser = filingProvider.filingArcsParser()
    private val conceptManager = filingProvider.conceptManager()
    private val labelManager = filingProvider.labelManager()

    /*
    Declare frequently used reference data
     */
    private val facts = facts(filingProvider)
    private val dimensions = filingProvider.incomeStatementDeclaredDimensions()
    private val calculations = filingArcsParser.parseFilingArcs()

    /*
    The follow statements translate Arcs -> Items
     */
    private val incomeStatementItems = calculations
        .incomeStatement
        .flatMap { arc ->
            arcToItems(arc)
        }

    private val balanceSheetItems = calculations
        .balanceSheet
        .flatMap { arc ->
            arcToItems(arc)
        }

    /*
    ---------------------
    Constants Hard Coding
    ---------------------
     */
    private val revenueConceptNameCandidates = setOf(
        "Revenues",
        "RevenueFromContractWithCustomerExcludingAssessedTax",
        "RevenueFromContractWithCustomerIncludingAssessedTax",
    )

    private val netIncomeLossConceptNameCandidates = setOf(
        "NetIncomeLoss",
        "NetIncomeLossAvailableToCommonStockholdersBasic",
        "ProfitLoss",
    )

    /*
    Find the net income / revenue item etc.
     */
    private val netIncomeItem = incomeStatementItems
        .reversed()
        .find { netIncomeLossConceptNameCandidates.contains(it.name) } ?: error("netincome item cannot be found")

    private val revenueItem = revenueItemReverseBfs(incomeStatementItems) ?: error("revenue item cannot be found")

    private val avgSharesOutstandingBasicAndDiluted = "WeightedAverageNumberOfShareOutstandingBasicAndDiluted"
    private val avgSharesOutstandingBasic = "WeightedAverageNumberOfSharesOutstandingBasic"
    private val avgSharesOutstandingDiluted = "WeightedAverageNumberOfDilutedSharesOutstanding"

    /*
    ----------------------------
    End of Constants Hard Coding
    ----------------------------
     */

    /**
     *  Parse the filing to create [Item]s from the calculation arcs
     *  declared on the Xbrl files [GenerateItemsResult]
     */
    fun generateItems(): GenerateItemsResult {

        /*
        create and then replace existing EPS item(s) with the ones below
         */
        val epsBasic = earningsPerShareBasic()
        val epsDiluted = earningsPerShareDiluted()
        val epsBasicAndDiluted = earningsPerShareBasicAndDiluted()

        val incomeStatementItemsWithEpsReplaced =
            listOfNotNull(epsBasic, epsDiluted, epsBasicAndDiluted).fold(incomeStatementItems) { acc, item ->
                if (acc.any { it.name == item.name }) {
                    acc.map {
                        if (it.name == item.name) {
                            item
                        } else {
                            it
                        }
                    }
                } else {
                    acc
                }
            }


        /*
        if any of the shares outstanding item is newly created
        then tag them onto the end of the income statement
         */
        val basicSharesOutstanding = epsBasic
            ?.let { findOrCreate(avgSharesOutstandingBasic) }
        val dilutedSharesOutstanding = epsDiluted
            ?.let { findOrCreate(avgSharesOutstandingDiluted) }
        val basicAndDilutedSharesOutstanding = epsBasicAndDiluted
            ?.let { findOrCreate(avgSharesOutstandingBasicAndDiluted) }

        val incomeStatementItemsWithSharesOutstanding = listOfNotNull(
            basicSharesOutstanding,
            dilutedSharesOutstanding,
            basicAndDilutedSharesOutstanding,
        ).fold(incomeStatementItemsWithEpsReplaced) { acc, item ->
            if (acc.any { it.name == item.name }) {
                acc
            } else {
                acc + item
            }
        }

        /*
        clean up references that does not exist
         */
        val lookup = (incomeStatementItemsWithSharesOutstanding + balanceSheetItems).associateBy { it.name }
        val cleanedIncomeStatement = incomeStatementItemsWithSharesOutstanding.map { item ->
            if (item.sumOfOtherItems != null) {
                val filtered =
                    item.sumOfOtherItems.components.filter { component -> lookup[component.itemName] != null }
                item.copy(sumOfOtherItems = item.sumOfOtherItems.copy(components = filtered))
            } else {
                item
            }
        }

        val cleanedBalanceSheet = balanceSheetItems.map { item ->
            if (item.sumOfOtherItems != null) {
                val filtered =
                    item.sumOfOtherItems.components.filter { component -> lookup[component.itemName] != null }
                item.copy(sumOfOtherItems = item.sumOfOtherItems.copy(components = filtered))
            } else {
                item
            }
        }

        return GenerateItemsResult(
            revenue = revenueItem,
            netIncome = netIncomeItem,
            incomeStatementItems = cleanedIncomeStatement,
            balanceSheetItems = cleanedBalanceSheet,

            epsBasic = epsBasic,
            epsDiluted = epsDiluted,
            epsBasicAndDiluted = epsBasicAndDiluted,

            basicSharesOutstanding = basicSharesOutstanding,
            dilutedSharesOutstanding = dilutedSharesOutstanding,
            basicAndDilutedSharesOutstanding = basicAndDilutedSharesOutstanding,
        )

    }

    /**
     * Performs the primary task of turning an [Arc] into an [Item]
     *
     * @param arc the [Arc] to be turned into [Item]
     */
    private fun arcToItems(arc: Arc): List<Item> {
        /*
        Step 1 - find the facts that we will itemize associated with this Arc's concept
        this is either the dimensionless item or we've split it up by some dimension
         */
        val dimensionalFacts =
            findDimensionToUse(dimensions, facts)
                ?.let { facts.filter { fact -> fact.conceptName == arc.conceptName }.filterForDimension(it) }
                ?: emptyList()
        val dimensionlessFact = facts.filter { it.conceptName == arc.conceptName }.find { it.explicitMembers.isEmpty() }

        /*
        short circuit the process if dimensional and dimensionless facts
        are not found and there are no associated calculations - this mean this fact has no value
         */
        if (dimensionalFacts.isEmpty()
            && dimensionlessFact == null
            && arc.calculations.isEmpty()
        ) {
            return emptyList()
        }

        /*
        Step 2 - create dimensional items to the extent there are any
        dimensional facts
         */
        val dimensionalItems = dimensionalFacts.map { fact ->
            val labels = dimensionMemberLabel(fact)
            Item(
                name = itemNameGenerator.itemName(fact),
                description = labelWaterfall(labels),
                type = ItemType.FixedCost,
                historicalValue = historicalValue(fact),
                fixedCost = FixedCost(fact.doubleValue.orZero())
            )
        }

        /*
        Step 3 - create the dimensionless item from the dimensional items
        and handle cases where the dimensionless item is not backed by a fact (i.e. a lookup for the corresponding concept
        without a dimension returns nothing)
         */

        /*
        Step 3.1 - Generate the Sum property that will go on our dimensionless Item
        the Sum property consists of two kinds of constituents:

        - Explicitly declared dependency on other Concepts via the calculationArcs
        - Dimensional items found above

         */
        val explicitCalculationComponents = arc
            .calculations
            .map { calculation ->
                Component(
                    weight = calculation.weight,
                    itemName = itemNameGenerator.itemName(calculation.conceptName)
                )
            }
        val dimensionalComponents = dimensionalItems.map { item -> Component(weight = 1.0, itemName = item.name) }
        val sum = SumOfOtherItems(explicitCalculationComponents + dimensionalComponents)

        /*
        Step 3.2 - Generate the dimensionless Item
        this can be complicated by the fact that there might not be an Fact backing this Item
         */
        val labels = conceptManager.getConcept(arc.conceptHref)?.id?.let { labelManager.getLabel(it) }
        val dimensionlessItem = if (dimensionlessFact == null && dimensionalFacts.isNotEmpty()) {
            /*
            If no fact can be found to support the dimensionless item
            we must create one from scratch, this happens if the filing entity
            only provide dimensional data
             */
            val historicalValue = historicalValue(dimensionalFacts)
            Item(
                name = itemNameGenerator.itemName(arc.conceptName),
                description = labelWaterfall(labels),
                historicalValue = historicalValue
            )
        } else if (dimensionlessFact != null) {
            /*
            Else, we are in the case where there is a Fact that backs
            this dimensionless item
             */
            val historicalValue = historicalValue(dimensionlessFact)
            Item(
                name = itemNameGenerator.itemName(dimensionlessFact),
                description = labelWaterfall(labels),
                historicalValue = historicalValue,
            )
        } else {
            Item(
                name = itemNameGenerator.itemName(arc.conceptName),
                description = labelWaterfall(labels),
            )
        }

        /*
        Step 3.3 - Set the formula for the dimensionless Item
        if it has components (either explicitly declared or through dimensional decomposition) then
        we assign it a [Sum] property otherwise it's formula will be set to its most recent historical value
         */
        val dlessItemWithFormula = if (sum.components.isEmpty()) {
            dimensionlessItem.copy(
                type = ItemType.FixedCost,
                fixedCost = FixedCost(dimensionlessItem.historicalValue?.value.orZero()),
                subtotal = false
            )
        } else {
            dimensionlessItem.copy(type = ItemType.SumOfOtherItems, sumOfOtherItems = sum, subtotal = true)
        }

        return dimensionalItems + dlessItemWithFormula
    }

    private fun labelWaterfall(labels: Labels?) =
        labels?.terseLabel ?: labels?.label ?: labels?.verboseLabel

    /**
     * Derive [Labels] from the [Fact], assuming the fact
     * being presented have dimensions, if not the conceptId of the fact itself
     * should be used to derive labels
     */
    private fun dimensionMemberLabel(fact: Fact): Labels? {
        val parts = fact.explicitMembers.first().value.split(":")
        val namespace = parts[0]
        val conceptName = parts[1]
        val longNs = filingProvider.instanceDocument().shortNamespaceToLongNamespaceMap()[namespace]
        val conceptId = filingProvider.conceptManager().getConcept(longNs!!, conceptName)?.id
        return conceptId?.let { labelManager.getLabel(it) }
    }

    /**
     * Turn [facts] into a [HistoricalValue]
     */
    private fun historicalValue(facts: List<Fact>): HistoricalValue {
        val fact = facts.first()
        return HistoricalValue(
            factIds = facts.map { fact -> fact._id },
            conceptName = fact.conceptName,
            value = facts.sumByDouble { it.doubleValue.orZero() },
            documentFiscalPeriodFocus = fact.documentFiscalPeriodFocus.toString(),
            documentPeriodEndDate = fact.documentPeriodEndDate.toString(),
            documentFiscalYearFocus = fact.documentFiscalYearFocus,
        )
    }

    private fun historicalValue(fact: Fact?): HistoricalValue? {
        return if (fact == null) {
            null
        } else
            HistoricalValue(
                factId = fact._id,
                conceptName = fact.conceptName,
                value = fact.doubleValue.orZero(),
                documentFiscalPeriodFocus = fact.documentFiscalPeriodFocus.toString(),
                documentPeriodEndDate = fact.documentPeriodEndDate.toString(),
                documentFiscalYearFocus = fact.documentFiscalYearFocus,
            )
    }

    // TODO tighten up the logic here, how do we know what are the correct combinations of dimensions to use
    private fun findDimensionToUse(dimensions: List<Dimension>, facts: List<Fact>): Dimension? {
        return dimensions.find { dimension ->
            /*
            choose the current dimension if there are facts matching all of its members
             */
            val filtered = facts.filter { fact ->
                explicitMembersMatchDimension(fact.explicitMembers, dimension)
            }
            filtered.isNotEmpty()
        }
    }


    /**
     * Determine if the passed in [XbrlExplicitMember]s match
     */
    private fun explicitMembersMatchDimension(
        explicitMembers: List<XbrlExplicitMember>,
        dimension: Dimension
    ): Boolean {
        return if (explicitMembers.size != 1) {
            false
        } else {
            val explicitMember = explicitMembers.first()
            (explicitMember.dimension == dimension.dimensionConcept
                    && dimension.memberConcepts.contains(explicitMember.value))
        }
    }

    /**
     * Get the latest facts for a given concept for a given filer
     */
    private fun facts(filingProvider: FilingProvider): List<Fact> {
        return filingProvider.factsParser().parseFacts().facts
    }


    /**
     * ## What is this?
     * Identify which item among the given items is a Revenue item
     * this is done via a reverse breath-first-search
     *
     * We start with NetIncome and then walk backwards from the calculation tree
     * (exhausting the search on current layer)
     *
     * ## Why a BFS?
     * We cannot simply filter on a list of candidate concepts for Revenue
     * as companies are idiotic and use multiple tags to represent revenue, that are sometimes
     * completely in parallel to each other with no calculationArcs or presentationArcs bridging them
     */
    private fun revenueItemReverseBfs(
        items: List<Item>
    ): Item? {
        val lookup = items.associateBy { it.name }

        /**
         * This inner helper function help translate dependencies
         * between items
         * @param components
         */
        fun componentsToItem(components: List<Component>? = null): List<Item> {
            if (components == null) {
                return emptyList()
            }
            return components.mapNotNull { component ->
                val itemName = component.itemName
                lookup[itemName]
            }
        }

        val queue = ArrayDeque<Item>()
        queue.addAll(componentsToItem(netIncomeItem.sumOfOtherItems?.components))
        var revenueItem: Item? = null
        while (queue.isNotEmpty()) {
            val item = queue.removeFirst()
            /*
            we've found the revenue item if this item is the first item that match one of the revenue
            conceptNames
             */
            val itemName = item.name
            if (revenueConceptNameCandidates.contains(itemName)) {
                revenueItem = item
                break
            } else {
                queue.addAll(componentsToItem(item.sumOfOtherItems?.components))
            }
        }
        return revenueItem
    }

    /**
     * # What is this?
     * Creates an [Item] from the given concept name
     * if an [Item] has not been created by this instance of [ItemGenerator]
     * during constructor invocation
     *
     * # Why is it used?
     * This is used to find / create weighted average shares outstanding items,
     * which are sometimes not declared on income statements and thus are not
     * automatically created when we looped through income statement arcs to create items
     */
    private fun findOrCreate(conceptName: String): Item? {
        val found = (incomeStatementItems + balanceSheetItems).find { item -> item.name == conceptName }
        val itemName = itemNameGenerator.itemName(conceptName)
        // no need to create this item
        return if (found != null) {
            return found
        } else {
            val fact = facts.find { it.conceptName == conceptName && it.explicitMembers.isEmpty() } ?: return null
            val historicalValue = historicalValue(fact)
            Item(
                name = itemName,
                historicalValue = historicalValue,
                description = itemName,
                type = ItemType.FixedCost,
                fixedCost = FixedCost(historicalValue?.value.orZero()),
            )
        }
    }

    private fun earningsPerShareBasic(): Item? {
        val items = incomeStatementItems.reversed()
        val ret = items.find { item -> item.name == "EarningsPerShareBasic" }
            ?: items.find { item -> item.name == "IncomeLossFromContinuingOperationsPerBasicShare" }
        return ret?.copy(
            type = ItemType.Custom,
            formula = "${netIncomeItem.name}/${itemNameGenerator.itemName(avgSharesOutstandingBasic)}",
        )
    }

    private fun earningsPerShareDiluted(): Item? {
        val items = incomeStatementItems.reversed()
        val ret = items.find { item -> item.name == "EarningsPerShareDiluted" }
            ?: items.find { item -> item.name == "IncomeLossFromContinuingOperationsPerDilutedShare" }
        return ret?.copy(
            type = ItemType.Custom,
            formula = "${netIncomeItem.name}/${itemNameGenerator.itemName(avgSharesOutstandingDiluted)}",
        )
    }

    private fun earningsPerShareBasicAndDiluted(): Item? {
        val items = incomeStatementItems.reversed()
        val ret = items.find { item -> item.name == "EarningsPerShareBasicAndDiluted" }
        return ret?.copy(
            type = ItemType.Custom,
            formula = "${netIncomeItem.name}/${itemNameGenerator.itemName(avgSharesOutstandingBasicAndDiluted)}",
        )
    }

}