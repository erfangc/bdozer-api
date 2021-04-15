package com.bdozer.models.translator.subtypes

import com.bdozer.models.translator.FormulaTranslationContext
import com.bdozer.spreadsheet.Cell

class CustomTranslator(private val ctx: FormulaTranslationContext) : FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val expression = cell.item.formula
        return GenericTranslator(ctx)
            .translateFormula(cell.copy(formula = expression))
    }
}
