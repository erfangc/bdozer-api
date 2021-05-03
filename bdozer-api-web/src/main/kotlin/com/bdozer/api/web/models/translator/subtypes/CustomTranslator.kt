package com.bdozer.api.web.models.translator.subtypes

import com.bdozer.api.web.models.translator.FormulaTranslationContext
import bdozer.api.common.spreadsheet.Cell

class CustomTranslator(private val ctx: FormulaTranslationContext) : FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val expression = cell.item.formula
        return GenericTranslator(ctx)
            .translateFormula(cell.copy(formula = expression))
    }
}
