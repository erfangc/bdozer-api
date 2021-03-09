package com.starburst.starburst.edgar.factbase.modelbuilder

import com.starburst.starburst.edgar.factbase.Period
import com.starburst.starburst.models.HistoricalValue
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model

/**
 * This is the brains of the automated valuation model
 */
class ModelFormulaBuilder(
    private val model: Model,
    private val ctx: ModelFormulaBuilderContext
) {

    /**
     * Takes as input model that is already linked via the calculationArcs
     * and with historical values for the items populated
     *
     * This is the master method for which we begin to populate formulas and move them beyond
     * simply 0.0 or repeating historical
     */
    fun buildModelFormula(): Model {

        val formualtedIncomeStatementItems = model.incomeStatementItems.map { item ->
            /*
            for any items for which there exists an existing calculationArc specified
            calculation return it without further processing
             */
            val formulatedItem = when {
                ctx.itemDependencyGraph[item.name]
                    ?.isNotEmpty() == true -> {
                    item
                }
                /*
                treat revenue item(s) using revenue like formulas
                 */
                isRevenue(item) -> {
                    formulateRevenueItem(item)
                }
                /*
                if this is a cost item that should be revenue driven then
                process it using [formulateRevenueDrivenItem]
                 */
                isRevenueDriven(item) -> {
                    formulateRevenueDrivenItem(item)
                }
                isTotalAssetDriven(item) -> {
                    formulateTotalAssetDrivenItem(item)
                }
                isFixed(item) -> {
                    formulateFixedCostItem(item)
                }
                isOneTime(item) -> {
                    formulateOneTimeItem(item)
                }
                else -> {
                    item
                }
            }

            /*
            Determine if the item is something that requires some additional process
            and flagging

            Here we use private extension classes to chain them linearly rather than
            nesting the processing

            Taking advantage of a great but controversial Kotlin lang feature
            for readability - please do not export these chain functions, as they make no semantic sense
            outside the context of this class
             */
            formulatedItem
                .stockBasedCompensationChain()
                .nonCashChain()
        }

        return model
    }

    private fun Item.stockBasedCompensationChain(): Item {
        return if (this.name == "ShareBasedCompensation") {
            this.copy(stockBasedCompensation = true)
        } else {
            this
        }
    }

    private fun extractTimeSeries(itemName: String, type: Period): List<HistoricalValue> {
        val revenueItem = model.incomeStatementItems.find { it.name == itemName } ?: error("...")
        return when (type) {
            Period.ANNUAL -> {
                revenueItem.historicalValues?.fiscalYear ?: emptyList()
            }
            Period.QUARTER -> {
                revenueItem.historicalValues?.quarterly ?: emptyList()
            }
            Period.LTM -> {
                revenueItem.historicalValues?.ltm?.let { listOf(it) } ?: emptyList()
            }
        }
    }

    /**
     * helper to extract revenue time series and line up dates with some other time series
     */
    private fun alignItemToRevenue(item: Item, type: Period? = null): List<Pair<HistoricalValue, HistoricalValue>> {
        return if (type == null) {
            /*
            in this case, lets try out each one in order, only produce something that seem to have reasonable number
            of data points
             */
            val annual = alignItemToRevenueForPeriod(item, Period.ANNUAL)
            if (annual.size > 2) {
                annual
            } else {
                alignItemToRevenueForPeriod(item, Period.QUARTER)

            }
        } else {
            alignItemToRevenueForPeriod(item, type)
        }
    }

    /**
     * helper to extract revenue time series and line up dates with some other time series
     */
    private fun alignItemToRevenueForPeriod(item: Item, type: Period): List<Pair<HistoricalValue, HistoricalValue>> {
        val revenueTs = extractTimeSeries(itemName = expressionForTotalRevenue(), type = type)
        val itemTs = extractTimeSeries(itemName = item.name, type = type)
        val lookup = itemTs.associateBy { it.documentPeriodEndDate }
        return revenueTs.mapNotNull { ts ->
            val historicalValue = lookup[ts.endDate]
            if (historicalValue == null) {
                null
            } else {
                ts to historicalValue
            }
        }
    }

    private fun isDebtFlowItem(item: Item): Boolean {
        // we find the element definition
        val elementDefinition = ctx.elementDefinitionMap[item.name]
        val balance = elementDefinition?.balance
        val abstract = elementDefinition?.abstract
        val periodType = elementDefinition?.periodType
        val type = elementDefinition?.type

        /*
        A non cash expense is one that
        1. is a non-abstract monetary debit item as defined by the company's XSD or us-gaap
        2. starts or ends with the correct keywords
         */
        return (balance == "debit"
                && abstract != true
                && periodType == "duration"
                && type?.endsWith("monetaryItemType") == true)
    }


    private fun isCreditFlowItem(item: Item): Boolean {
        // we find the element definition
        val elementDefinition = ctx.elementDefinitionMap[item.name]
        val balance = elementDefinition?.balance
        val abstract = elementDefinition?.abstract
        val periodType = elementDefinition?.periodType
        val type = elementDefinition?.type

        /*
        A non cash expense is one that
        1. is a non-abstract monetary debit item as defined by the company's XSD or us-gaap
        2. starts or ends with the correct keywords
         */
        return (balance == "credit"
                && abstract != true
                && periodType == "duration"
                && type?.endsWith("monetaryItemType") == true)
    }

    private fun Item.nonCashChain(): Item {
        val itemName = this.name
        /*
        A non cash expense is one that
        1. is a non-abstract monetary debit item as defined by the company's XSD or us-gaap
        2. starts or ends with the correct keywords
         */
        val isDebitFlowItem = isDebtFlowItem(this)

        // next test whether it matches the right vocab
        val keywords = listOf("amortization", "depreciation", "impairment")
        val hasDesiredKeyword = keywords.any { keyword ->
            itemName.toLowerCase().startsWith(keyword)
                    || itemName.toLowerCase().endsWith(keyword)
        }

        return if (isDebitFlowItem && hasDesiredKeyword) {
            this.copy(nonCashExpense = true)
        } else {
            this
        }
    }

    private fun formulateFixedCostItem(item: Item): Item {
        return item.copy(expression = "${item.historicalValue}")
    }

    private fun isFixed(item: Item): Boolean {
        /*
        An item is considered a fixed expense if it does not appear to vary at all with revenue
         */
        val ts = alignItemToRevenue(item)
        if (ts.size < 3) {
            //
            // to be safe, if there is not enough data points
            // just assume this is not a fixed item
            //
            return false
        } else {
            //
            // an item is considered fixed if it does not appear to vary with revenue at all
            //
            ts.map { it.first.value ?: 0.0 }
        }
        TODO("Not yet implemented")
    }

    private fun formulateOneTimeItem(item: Item): Item {
        return item.copy(expression = "0.0")
    }

    private fun isOneTime(item: Item): Boolean {
        TODO("Not yet implemented")
    }

    private fun isRevenue(item: Item): Boolean {
        // for now assume that any balance=credit is a revenue item
        val balance = ctx.elementDefinitionMap[item.name]?.balance
        val periodType = ctx.elementDefinitionMap[item.name]?.periodType
        val type = ctx.elementDefinitionMap[item.name]?.type ?: ""
        return balance == "credit" && periodType == "duration" && type.endsWith("monetaryItemType")
    }

    /**
     * Figure out the correct [Item.expression] for total revenue for this company
     * if a total revenue line is already defined, then use that otherwise create a summation expression
     * of the individual components
     */
    private fun expressionForTotalRevenue(): String {
        // see if the standard set of GAAP total revenue metrics are defined
        return listOf(
            "Revenues",
            "RevenueFromContractWithCustomerExcludingAssessedTax",
            "RevenuesNetOfInterestExpense",
        ).find { itemName -> ctx.itemDependencyGraph.containsKey(itemName) && ctx.itemDependencyGraph[itemName]!!.isNotEmpty() }
            ?: error("Unable to find a total revenue item")
    }

    private fun formulateRevenueItem(item: Item): Item {
        TODO()
    }

    private fun isRevenueDriven(item: Item): Boolean {
        TODO()
    }

    private fun formulateRevenueDrivenItem(item: Item): Item {
        TODO()
    }

    private fun isTotalAssetDriven(item: Item): Boolean {
        TODO()
    }

    private fun formulateTotalAssetDrivenItem(item: Item): Item {
        TODO()
    }

}