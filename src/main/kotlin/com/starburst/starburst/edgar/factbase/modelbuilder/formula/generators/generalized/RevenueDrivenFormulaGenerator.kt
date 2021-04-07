package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.generalized

import com.starburst.starburst.extensions.DoubleExtensions.fmtPct
import com.starburst.starburst.extensions.DoubleExtensions.fmtRound
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.LinearRegression
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ElementSemanticsExtensions.isDebtFlowItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ItemValueExtractorsExtension.itemTimeSeriesVsRevenue
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.NameExpressionExtensions.totalRevenueExpression
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.FormulaGenerator
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.Result
import com.starburst.starburst.models.dataclasses.Item
import org.springframework.stereotype.Service

@Service
class RevenueDrivenFormulaGenerator : FormulaGenerator {

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
            Result(
                item = item,
                commentary = "This item does not have enough historical data, we will assume it's the same value going forward"
            )
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
                    item = item.copy(formula = "$slope*(${ctx.totalRevenueExpression()})${if (intercept > 0) "+$intercept" else intercept}"),
                    commentary = commentary
                )
            } else {
                Result(item = item, commentary = "This expense historically has not been increasing with revenue")
            }
        }
    }

    /**
     * this one is only relevant if the item has not been touched
     */
    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        return ctx.isDebtFlowItem(item)
                && ctx.isDependentOn(item.name, "OperatingIncomeLoss")
    }

}