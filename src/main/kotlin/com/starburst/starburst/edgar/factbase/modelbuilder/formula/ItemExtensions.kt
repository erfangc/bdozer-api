package com.starburst.starburst.edgar.factbase.modelbuilder.formula

import com.starburst.starburst.edgar.factbase.Period
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ElementSemanticsExtensions.isDebtFlowItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderExtensions.expressionForTotalRevenue
import com.starburst.starburst.models.HistoricalValue
import com.starburst.starburst.models.Item


internal object ItemExtensions {

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

    fun Item.nonCashChain(builder: ModelFormulaBuilder): Item {
        val itemName = this.name
        /*
        A non cash expense is one that
        1. is a non-abstract monetary debit item as defined by the company's XSD or us-gaap
        2. starts or ends with the correct keywords
         */
        val isDebitFlowItem = builder.isDebtFlowItem(this)

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

    fun Item.stockBasedCompensationChain(): Item {
        return if (this.name == "ShareBasedCompensation") {
            this.copy(stockBasedCompensation = true)
        } else {
            this
        }
    }

}