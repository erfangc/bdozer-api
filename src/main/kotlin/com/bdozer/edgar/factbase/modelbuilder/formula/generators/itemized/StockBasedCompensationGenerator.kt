package com.bdozer.edgar.factbase.modelbuilder.formula.generators.itemized

import com.bdozer.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.bdozer.edgar.factbase.modelbuilder.formula.USGaapConstants.ShareBasedCompensation
import com.bdozer.edgar.factbase.modelbuilder.formula.generators.FormulaGenerator
import com.bdozer.edgar.factbase.modelbuilder.formula.generators.Result
import com.bdozer.models.dataclasses.Item
import org.springframework.stereotype.Service

@Service
class StockBasedCompensationGenerator : FormulaGenerator {

    val commentary = """
    Share based compensation does not require cash up-front from the company. However when these shares vest
    they dilute earning from existing shareholders
    """.trimMargin()

    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
        return Result(item.copy(stockBasedCompensation = true), commentary = commentary)
    }

    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        return item.name == ShareBasedCompensation
    }

}