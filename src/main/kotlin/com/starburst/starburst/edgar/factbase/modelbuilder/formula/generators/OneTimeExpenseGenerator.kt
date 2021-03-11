package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ElementSemanticsExtensions.isDebtFlowItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.models.Item

class OneTimeExpenseGenerator : FormulaGenerator {
    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Item {
        val isOneTime = ctx.isDebtFlowItem(item)
                && item.name.toLowerCase().contains("impairment")
        return if (isOneTime) {
            item.copy(expression = "0.0")
        } else {
            item
        }
    }
}