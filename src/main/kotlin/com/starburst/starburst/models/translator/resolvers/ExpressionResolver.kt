package com.starburst.starburst.models.translator.resolvers

import com.starburst.starburst.cells.Cell

interface ExpressionResolver {
    fun resolveExpression(cell: Cell): Cell
}
