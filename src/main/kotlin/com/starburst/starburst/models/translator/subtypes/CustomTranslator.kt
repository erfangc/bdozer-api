package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.spreadsheet.Cell

class CustomTranslator(private val ctx: ResolverContext): FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val expression = cell.item.expression
        return GenericTranslator(ctx)
            .translateFormula(cell.copy(formula = expression))
    }
}
