package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.spreadsheet.Cell

class CustomExpressionTranslator(private val ctx: ResolverContext): ExpressionTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val expression = cell.driver?.customDriver?.formula ?: error("formula is required for custom cells")
        return GenericExpressionTranslator(ctx)
            .translateFormula(cell.copy(formula = expression))
    }
}
