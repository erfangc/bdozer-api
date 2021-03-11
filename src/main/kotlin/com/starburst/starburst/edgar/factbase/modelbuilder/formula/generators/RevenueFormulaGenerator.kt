package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ElementSemanticsExtensions.isCreditFlowItem
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Util.previous

class RevenueFormulaGenerator : FormulaGenerator {

    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
        return Result(
            item = item.copy(
                expression = "${previous(item.name)} * (1+15%)"
            )
        )
    }

    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        return ctx.isCreditFlowItem(item)
    }

}