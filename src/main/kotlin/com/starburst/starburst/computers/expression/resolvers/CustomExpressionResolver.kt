package com.starburst.starburst.computers.expression.resolvers

import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.models.Cell

data class CustomDriver(
    val formula: String = "0"
)

class CustomExpressionResolver(private val ctx: ResolverContext): ExpressionResolver {
    override fun resolveExpression(cell: Cell): Cell {
        val expression = cell.driver?.customDriver?.formula ?: error("")
        return StringExpressionResolver(ctx)
            .resolveExpression(cell.copy(expression = expression))
    }
}
