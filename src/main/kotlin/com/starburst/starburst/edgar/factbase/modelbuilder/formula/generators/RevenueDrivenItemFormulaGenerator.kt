package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ElementSemanticsExtensions.isDebtFlowItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ItemExtensions.itemTimeSeriesVsRevenue
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.LinearRegression
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderExtensions.expressionForTotalRevenue
import com.starburst.starburst.models.Item

class RevenueDrivenItemFormulaGenerator : FormulaGenerator{
    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Item {
        /*
         An item is considered revenue driven if it correlates heuristically with
         revenue
          */
        val timeSeries = ctx.itemTimeSeriesVsRevenue(item)
        return if (timeSeries.size < 3) {
            /*
            to be safe, if there is not enough data points
            just assume the item scales with revenue
             */
            item
        } else {
            /*
            an item is considered fixed if it does not appear to vary with revenue at all
             */
            return if (ctx.isDebtFlowItem(item)) {
                val x = timeSeries.map { it.first.value ?: 0.0 }.toDoubleArray()
                val y = timeSeries.map { it.second.value ?: 0.0 }.toDoubleArray()
                val linest = LinearRegression(x, y)
                val slope = linest.slope()
                if (slope > 0) {
                    item.copy(expression = "$slope*(${ctx.expressionForTotalRevenue()})+${linest.intercept()}")
                } else {
                    item
                }
            } else {
                item
            }
        }
    }
}