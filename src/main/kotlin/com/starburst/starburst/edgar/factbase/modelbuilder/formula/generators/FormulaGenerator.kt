package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.models.Item

interface FormulaGenerator {
    fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result
    fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean
}