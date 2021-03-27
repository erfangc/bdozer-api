package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.itemized

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ElementSemanticsExtensions.isCreditFlowItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.FormulaGenerator
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.Result
import com.starburst.starburst.models.Utility.previous
import com.starburst.starburst.models.dataclasses.Item
import org.springframework.stereotype.Service

@Service
class RevenueFormulaGenerator : FormulaGenerator {

    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
        return Result(
            item = item.copy(
                formula = "${previous(item.name)} * (1+15%)"
            )
        )
    }

    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        return ctx.isCreditFlowItem(item)
    }

}