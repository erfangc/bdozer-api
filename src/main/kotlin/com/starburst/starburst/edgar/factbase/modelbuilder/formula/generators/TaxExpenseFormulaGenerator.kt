package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EBT
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.IncomeTaxExpenseBenefit
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ItemValueExtractorsExtension.item
import com.starburst.starburst.models.Item
import java.lang.Double.min

class TaxExpenseFormulaGenerator : FormulaGenerator {
    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Item {
        return if (item.name == IncomeTaxExpenseBenefit) {
            return if (hasIncomeFromContinuingOperations(ctx)) {
                val ebit = ctx.item(EBT)
                val taxRate = min(
                    0.15,
                    item.historicalValue / (ebit?.historicalValue ?: Double.MAX_VALUE)
                )
                item.copy(expression = "$EBT*$taxRate")
            } else {
                item
            }
        } else {
            item
        }
    }

    private fun hasIncomeFromContinuingOperations(ctx: ModelFormulaBuilderContext) =
        ctx.itemDependencyGraph.containsKey(EBT)
}