package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.generalized

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ElementSemanticsExtensions.isDebtFlowItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.FormulaGenerator
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.Result
import com.starburst.starburst.models.dataclasses.Item
import org.springframework.stereotype.Service

@Service
class OneTimeExpenseGenerator : FormulaGenerator {
    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
        return Result(
            item = item.copy(expression = "0.0"),
            commentary = "This is a one time items that we do not expect to repeat"
        )
    }

    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        return (ctx.isDebtFlowItem(item)
                && item.name.toLowerCase().contains("impairment"))
    }
}
