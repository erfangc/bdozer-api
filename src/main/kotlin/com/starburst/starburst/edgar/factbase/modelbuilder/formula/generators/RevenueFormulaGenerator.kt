package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ElementSemanticsExtensions.isCreditFlowItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.models.Item

class RevenueFormulaGenerator: FormulaGenerator {
    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Item {
        return if (ctx.isCreditFlowItem(item)) {
            // TODO use a real revenue projection here
            item.copy(expression = "${item.historicalValue}")
        } else {
            item
        }
    }
}