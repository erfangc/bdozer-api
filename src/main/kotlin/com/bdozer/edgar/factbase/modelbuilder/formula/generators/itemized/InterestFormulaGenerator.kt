package com.bdozer.edgar.factbase.modelbuilder.formula.generators.itemized

import com.bdozer.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.bdozer.edgar.factbase.modelbuilder.formula.USGaapConstants.InterestExpense
import com.bdozer.edgar.factbase.modelbuilder.formula.generators.FormulaGenerator
import com.bdozer.edgar.factbase.modelbuilder.formula.generators.Result
import com.bdozer.models.dataclasses.Item
import org.springframework.stereotype.Service

@Service
class InterestFormulaGenerator : FormulaGenerator {
    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
        return Result(
            item = item,
            commentary = "Interest expense projection is not supported at the moment"
        )
    }

    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        return item.name == InterestExpense
    }
}