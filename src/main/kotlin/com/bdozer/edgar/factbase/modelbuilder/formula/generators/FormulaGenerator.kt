package com.bdozer.edgar.factbase.modelbuilder.formula.generators

import com.bdozer.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.bdozer.models.dataclasses.Item

interface FormulaGenerator {
    fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result
    fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean
}