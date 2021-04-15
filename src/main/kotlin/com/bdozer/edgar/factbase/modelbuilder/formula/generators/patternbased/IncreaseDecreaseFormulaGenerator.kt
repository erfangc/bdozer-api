package com.bdozer.edgar.factbase.modelbuilder.formula.generators.patternbased

import com.bdozer.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.bdozer.edgar.factbase.modelbuilder.formula.extensions.ElementSemanticsExtensions.isFlowItem
import com.bdozer.edgar.factbase.modelbuilder.formula.generators.FormulaGenerator
import com.bdozer.edgar.factbase.modelbuilder.formula.generators.Result
import com.bdozer.edgar.factbase.modelbuilder.formula.generators.generalized.AverageFormulaGenerator
import com.bdozer.edgar.factbase.modelbuilder.formula.generators.generalized.RevenueDrivenFormulaGenerator
import com.bdozer.models.dataclasses.Item
import org.springframework.stereotype.Service

@Service
class IncreaseDecreaseFormulaGenerator : FormulaGenerator {

    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
        // first see if this item varies with revenue - if not then use an average
        val result = RevenueDrivenFormulaGenerator()
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