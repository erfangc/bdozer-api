package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.generalized

import com.starburst.starburst.DoubleExtensions.fmtPct
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ElementSemanticsExtensions.isDebtFlowItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ItemValueExtractorsExtension.itemTimeSeriesVsRevenue
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.NameExpressionExtensions.totalRevenueExpression
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.FormulaGenerator
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.Result
import com.starburst.starburst.models.dataclasses.Item
import org.springframework.stereotype.Service

@Service
class PercentOfRevenueFormulaGenerator : FormulaGenerator {
    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
        val revenueExpression = ctx.totalRevenueExpression()
        val historical = ctx.itemTimeSeriesVsRevenue(item)
        val average = historical.map { (revenue, value) ->
            value.value!! / revenue.value!!
        }.average()
        return Result(
            item = item.copy(formula = "$revenueExpression*$average"),
            commentary = """
            Cost of goods sold is variable with respect to revenue, historically cost of goods
            sold has been ${average.fmtPct()}
            """.trimIndent()
        )
    }

    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        val isCogsMember = ctx.isDependentOn(item.name, USGaapConstants.CostOfGoodsAndServicesSold)
        return ctx.isDebtFlowItem(item) && isCogsMember
    }
}