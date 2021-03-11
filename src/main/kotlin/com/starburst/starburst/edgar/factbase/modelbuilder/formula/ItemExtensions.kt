package com.starburst.starburst.edgar.factbase.modelbuilder.formula

import com.starburst.starburst.edgar.factbase.Period
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderExtensions.expressionForTotalRevenue
import com.starburst.starburst.models.HistoricalValue
import com.starburst.starburst.models.Item


internal object ItemExtensions {

    /**
     * helper to extract revenue time series and line up dates with some other time series
     */
    fun ModelFormulaBuilderContext.itemTimeSeriesVsRevenueForPeriod(
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

    fun ModelFormulaBuilderContext.itemTimeSeries(itemName: String, type: Period): List<HistoricalValue> {
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
    fun ModelFormulaBuilderContext.itemTimeSeriesVsRevenue(
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

}