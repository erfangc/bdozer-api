package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.fmtPct
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ElementSemanticsExtensions.isDebtFlowItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ItemValueExtractorsExtension.itemTimeSeriesVsRevenue
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ModelFormulaBuilderExtensions.totalRevenueExpression
import com.starburst.starburst.models.Item

class CostOfGoodsSoldFormulaGenerator : FormulaGenerator {
    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
        val revenueExpression = ctx.totalRevenueExpression()
        val historical = ctx.itemTimeSeriesVsRevenue(item)
        val average = historical.map { (revenue, value) ->
            value.value!! / revenue.value!!
        }.average()
        return Result(
            item = item.copy(expression = "$revenueExpression*$average"),
            commentary = """
            Cost of goods sold is variable with respect to revenue, historically cost of goods
            sold has been ${average.fmtPct()}
            """.trimIndent()
        )
    }

    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        return ctx
            .isDebtFlowItem(item)
                && item
            .name
            .toLowerCase()
            .startsWith("costofgoods")
    }
}