package com.starburst.starburst.edgar.factbase.modelbuilder.formula

import com.starburst.starburst.edgar.factbase.Period
import com.starburst.starburst.models.HistoricalValue
import com.starburst.starburst.models.Item

/**
 * This is the brains of the automated valuation model
 */
object ModelFormulaBuilderExtensions {

    fun ModelFormulaBuilder.itemTimeSeries(itemName: String, type: Period): List<HistoricalValue> {
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
    fun ModelFormulaBuilder.itemTimeSeriesVsRevenue(
        item: Item,
        type: Period? = null
    ): List<Pair<HistoricalValue, HistoricalValue>> {
        return if (type == null) {
            /*
            in this case, lets try out each one in order, only produce something that seem to have reasonable number
            of data points
             */
            val annual = itemTimeSeriesVsRevenueForPeriod(item, Period.ANNUAL)
            if (annual.size > 2) {
                annual
            } else {
                itemTimeSeriesVsRevenueForPeriod(item, Period.QUARTER)

            }
        } else {
            itemTimeSeriesVsRevenueForPeriod(item, type)
        }
    }

    /**
     * helper to extract revenue time series and line up dates with some other time series
     */
    fun ModelFormulaBuilder.itemTimeSeriesVsRevenueForPeriod(
        item: Item,
        type: Period
    ): List<Pair<HistoricalValue, HistoricalValue>> {
        val revenueTs = itemTimeSeries(itemName = expressionForTotalRevenue(), type = type)
        val itemTs = itemTimeSeries(itemName = item.name, type = type)
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

    /**
     * Debit flow items are costs / expenses (those that are typically expected to be outflows_
     */
    fun ModelFormulaBuilder.isDebtFlowItem(item: Item): Boolean {
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


    /**
     * Cebit flow items are revenues / incomes (those that are typically expected to be inflows_
     */
    fun ModelFormulaBuilder.isCreditFlowItem(item: Item): Boolean {
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
                // != true is not the same as == false, as abstract could also be null
                && abstract != true
                && periodType == "duration"
                && type?.endsWith("monetaryItemType") == true)
    }

    fun ModelFormulaBuilder.formulateFixedCostItem(item: Item): Item {
        return item.copy(expression = "${item.historicalValue}")
    }

    fun ModelFormulaBuilder.formulateOneTimeItem(item: Item): Item {
        return item.copy(expression = "0.0")
    }

    fun ModelFormulaBuilder.isOneTime(item: Item): Boolean {
        // all impairment charges are one time charges
        return this.isDebtFlowItem(item)
                && item.name.toLowerCase().contains("impairment")
    }

    fun ModelFormulaBuilder.isRevenue(item: Item): Boolean {
        // for now assume that any balance=credit is a revenue item
        return isCreditFlowItem(item)
    }

    /**
     * Figure out the correct [Item.expression] for total revenue for this company
     * if a total revenue line is already defined, then use that otherwise create a summation expression
     * of the individual components
     */
    fun ModelFormulaBuilder.expressionForTotalRevenue(): String {
        // see if the standard set of GAAP total revenue metrics are defined
        return listOf(
            "Revenues",
            "RevenueFromContractWithCustomerExcludingAssessedTax",
            "RevenuesNetOfInterestExpense",
        ).find { itemName -> ctx.itemDependencyGraph.containsKey(itemName) }
            // TODO be more rigorous here
            ?: error("Unable to find a total revenue item")
    }

    fun ModelFormulaBuilder.formulateRevenueItem(item: Item): Item {
        //
        // TODO here to actually replace it with something that grows the revenue
        //
        return item.copy(expression = "${item.historicalValue}")
    }

    fun ModelFormulaBuilder.isRevenueDriven(item: Item): Boolean {
        /*
        An item is considered revenue driven if it correlates heuristically with
        revenue
         */
        val timeSeries = itemTimeSeriesVsRevenue(item)
        if (timeSeries.size < 3) {
            /*
            to be safe, if there is not enough data points
            just assume the item scales with revenue
             */
            return true
        } else {
            /*
            an item is considered fixed if it does not appear to vary with revenue at all
             */
            return if (this.isDebtFlowItem(item)) {
                val x = timeSeries.map { it.first.value ?: 0.0 }.toDoubleArray()
                val y = timeSeries.map { it.second.value ?: 0.0 }.toDoubleArray()
                val linest = LinearRegression(x, y)
                linest.slope() > 0
            } else {
                false
            }
        }
    }

    fun ModelFormulaBuilder.formulateRevenueDrivenItem(item: Item): Item {

        fun fixed(): Item {
            return item
        }

        /*
        An item is considered revenue driven if it correlates heuristically with
        revenue
         */
        val timeSeries = itemTimeSeriesVsRevenue(item)
        if (timeSeries.size < 2) {
            /*
            to be safe, if there is not enough data points
            just assume the item scales with revenue
             */
            return fixed()
        } else {
            /*
            an item is considered fixed if it does not appear to vary with revenue at all
             */
            return if (this.isDebtFlowItem(item)) {
                val x = timeSeries.map { it.first.value ?: 0.0 }.toDoubleArray()
                val y = timeSeries.map { it.second.value ?: 0.0 }.toDoubleArray()

                val linest = LinearRegression(x, y)
                val slope = linest.slope()
                val intercept = linest.intercept()

                return if (slope > 0) {
                    item.copy(expression = "$slope*(${expressionForTotalRevenue()})+$intercept")
                } else {
                    fixed()
                }
            } else {
                return fixed()
            }
        }
    }

    /**
     * Examples of this are interest expenses for assets
     */
    fun ModelFormulaBuilder.isTotalAssetDriven(item: Item): Boolean {
        // TODO figure out what to do here, for now nothing happens
        return false
    }

    fun ModelFormulaBuilder.formulateTotalAssetDrivenItem(item: Item): Item {
        TODO()
    }

}