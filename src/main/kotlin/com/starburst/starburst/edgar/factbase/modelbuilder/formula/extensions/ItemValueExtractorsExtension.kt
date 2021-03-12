package com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions

import com.starburst.starburst.edgar.factbase.Period
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.NameExpressionExtensions.totalRevenueExpression
import com.starburst.starburst.models.dataclasses.HistoricalValue
import com.starburst.starburst.models.dataclasses.Item


internal object ItemValueExtractorsExtension {

    /**
     * helper to extract revenue time series and line up dates with some other time series
     *
     * the 1nd element is Revenue
     * the 2st element in the pair is the Item's historical value
     *
     */
    fun ModelFormulaBuilderContext.itemTimeSeriesVsRevenueForPeriod(
        item: Item,
        type: Period
    ): List<Pair<HistoricalValue, HistoricalValue>> {
        val revenueTs = itemTimeSeries(itemName = totalRevenueExpression(), type = type)
        val itemTs = itemTimeSeries(itemName = item.name, type = type)
        val lookup = itemTs.associateBy { it.documentPeriodEndDate }
        return revenueTs.mapNotNull { revenueTs ->
            val historicalValue = lookup[revenueTs.endDate]
            if (historicalValue == null) {
                null
            } else {
                revenueTs to historicalValue
            }
        }
    }

    /**
     * helper to extract revenue time series and line up dates with some other time series
     *
     * the 1nd element is Revenue
     * the 2st element in the pair is the Item's historical value
     *
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

    /**
     * Return the original item before any formula generator could modify it
     */
    fun ModelFormulaBuilderContext.originalItem(name: String): Item? {
        val model = this.model
        return (model.incomeStatementItems +
                model.balanceSheetItems +
                model.cashFlowStatementItems).find { it.name == name }
    }

    fun ModelFormulaBuilderContext.itemTimeSeries(itemName: String, type: Period): List<HistoricalValue> {
        val items = model.incomeStatementItems + model.balanceSheetItems + model.cashFlowStatementItems
        val revenueItem = items.find { it.name == itemName } ?: error("unable to find $itemName")
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

    fun ModelFormulaBuilderContext.itemTimeSeries(itemName: String): List<HistoricalValue> {
        val annual = itemTimeSeries(itemName, Period.ANNUAL)
        return if (annual.size < 2) {
            itemTimeSeries(itemName, Period.QUARTER)
        } else {
            annual
        }
    }

}