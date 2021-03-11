package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.LinearRegression
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.fmtPct
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.fmtRound
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ElementSemanticsExtensions.isDebtFlowItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ItemValueExtractorsExtension.originalItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ItemValueExtractorsExtension.itemTimeSeriesVsRevenue
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ModelFormulaBuilderExtensions.expressionForTotalRevenue
import com.starburst.starburst.models.Item

class RevenueDrivenItemFormulaGenerator : FormulaGenerator {

    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
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
            Result(item = item, commentary = "This item does not have enough historical data, we will assume it's the same value going forward")
        } else {
            /*
            an item is considered fixed if it does not appear to vary with revenue at all
             */
            val x = timeSeries.map { it.first.value ?: 0.0 }.toDoubleArray()
            val y = timeSeries.map { it.second.value ?: 0.0 }.toDoubleArray()
            val lineEst = LinearRegression(x, y)
            val slope = lineEst.slope()
            if (slope > 0) {
                val intercept = lineEst.intercept()
                val commentary = """
                    This expense historically have been ${slope.fmtPct()} of revenue, on top of ${intercept.fmtRound()}
                """.trimIndent()
                Result(
                    item = item.copy(expression = "$slope*(${ctx.expressionForTotalRevenue()})+$intercept"),
                    commentary = commentary
                )
            } else {
                Result(item = item, commentary = "This expense historically has not been increasing with revenue")
            }
        }
    }

    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        /*
        this one is only relevant if the item has not been touched
         */
        val originalItem = ctx.originalItem(item.name)
        return if (item != originalItem) {
            false
        } else {
            ctx.isDebtFlowItem(item)
        }
    }

}