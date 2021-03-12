package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.itemized

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.IncomeTaxExpenseBenefit
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.fmtPct
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ItemValueExtractorsExtension.originalItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.FormulaGenerator
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.Result
import com.starburst.starburst.models.dataclasses.Item
import org.springframework.stereotype.Service
import java.lang.Double.max
import java.lang.Double.min
@Service
class TaxExpenseFormulaGenerator : FormulaGenerator {
    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
        return if (hasIncomeFromContinuingOperations(ctx)) {
            val ebit = ctx.originalItem(
                IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest
            )
            val effectiveTaxRate = item.historicalValue / (ebit?.historicalValue ?: error("..."))
            val taxRate = max(0.01, min(0.15, effectiveTaxRate))
            val commentary = """
                    The company has been taxed at ${effectiveTaxRate.fmtPct()} historically, 
                    we will assume ${taxRate.fmtPct()} going forward
                """.trimIndent()
            Result(
                item = item.copy(expression = "$IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest*$taxRate"),
                commentary = commentary
            )
        } else {
            Result(item = item)
        }
    }

    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        return item.name == IncomeTaxExpenseBenefit
    }

    private fun hasIncomeFromContinuingOperations(ctx: ModelFormulaBuilderContext) =
        ctx.itemDependencyGraph.containsKey(
            IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest
        )
}