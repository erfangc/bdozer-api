package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ElementSemanticsExtensions.isFlowItem
import com.starburst.starburst.models.Item

class IncreaseDecreaseFormulaGenerator : FormulaGenerator {

    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
        // first see if this item varies with revenue - if not then use an average
        val result = RevenueDrivenItemFormulaGenerator()
            .generate(item, ctx)
        return if (result.item != item) {
            result
        } else {
            AverageFormulaGenerator().generate(item, ctx)
        }
    }

    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        return ctx.isFlowItem(item) && item
            .name
            .toLowerCase()
            .startsWith("increasedecrease")
    }
}