package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderExtensions.getItem
import com.starburst.starburst.models.Item
import java.lang.Double.max

class TaxExpenseFormulaGenerator : FormulaGenerator{
    private val ebitName = "IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest"
    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Item {
        return if (item.name == "IncomeTaxExpenseBenefit") {
            return if (hasIncomeFromContinuingOperations(ctx)) {
                val ebit = ctx.getItem(ebitName)
                val taxRate = max(0.15, item.historicalValue / (ebit?.historicalValue ?: Double.MAX_VALUE))
                item.copy(expression = "$ebitName*$taxRate")
            } else {
                item
            }
        } else {
            item
        }
    }

    private fun hasIncomeFromContinuingOperations(ctx: ModelFormulaBuilderContext) =
        ctx.itemDependencyGraph.containsKey(ebitName)
}