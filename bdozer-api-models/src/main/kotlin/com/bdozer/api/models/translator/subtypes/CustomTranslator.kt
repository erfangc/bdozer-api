package com.bdozer.api.models.translator.subtypes

import com.bdozer.api.models.translator.FormulaTranslationContext
import com.bdozer.api.models.dataclasses.spreadsheet.Cell

class CustomTranslator(private val ctx: FormulaTranslationContext) : FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val expression = cell.item.formula
        return GenericTranslator(ctx)
            .translateFormula(cell.copy(formula = expression))
    }
}
