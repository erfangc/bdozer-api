package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.ShareBasedCompensation
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ElementSemanticsExtensions.isDebtFlowItem
import com.starburst.starburst.models.Item

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