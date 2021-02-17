package com.starburst.starburst.computers.expression.resolvers

import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.models.Cell

data class Custom(
    val expression: String
)

class CustomExpressionResolver(private val ctx: ResolverContext): ExpressionResolver {
    override fun resolveExpression(cell: Cell): Cell {
        val expression = cell.driver?.custom?.expression ?: error("")
        return ItemExpressionResolver(ctx)
            .resolveExpression(cell.copy(expression = expression))
    }
}
