package com.starburst.starburst.computers.expression.resolvers

import com.starburst.starburst.models.Cell

interface ExpressionResolver {
    fun resolveExpression(cell: Cell): Cell
}
