package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EBT
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.IncomeTaxExpenseBenefit
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.fmtPct
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ItemValueExtractorsExtension.originalItem
import com.starburst.starburst.models.Item
import java.lang.Double.max
import java.lang.Double.min

class TaxExpenseFormulaGenerator : FormulaGenerator {
    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
        return if (hasIncomeFromContinuingOperations(ctx)) {
            val ebit = ctx.originalItem(EBT)
            val effectiveTaxRate = item.historicalValue / (ebit?.historicalValue ?: error("..."))
            val taxRate = max(0.01, min(0.15, effectiveTaxRate))
            val commentary = """
                    The company has been taxed at ${effectiveTaxRate.fmtPct()} historically, 
                    we will assume ${taxRate.fmtPct()} going forward
                """.trimIndent()
            Result(item = item.copy(expression = "$EBT*$taxRate"), commentary = commentary)
        } else {
            Result(item = item)
        }
    }

    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        return item.name == IncomeTaxExpenseBenefit
    }

    private fun hasIncomeFromContinuingOperations(ctx: ModelFormulaBuilderContext) =
        ctx.itemDependencyGraph.containsKey(EBT)
}