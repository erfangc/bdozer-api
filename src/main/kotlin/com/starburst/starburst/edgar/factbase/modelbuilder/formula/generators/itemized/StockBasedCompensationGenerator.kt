package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.itemized

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.ShareBasedCompensation
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.FormulaGenerator
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.Result
import com.starburst.starburst.models.dataclasses.Item
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