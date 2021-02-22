package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.spreadsheet.Cell

class CustomExpressionTranslator(private val ctx: ResolverContext): ExpressionTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val expression = cell.item.expression
        return GenericExpressionTranslator(ctx)
            .translateFormula(cell.copy(formula = expression))
    }
}
