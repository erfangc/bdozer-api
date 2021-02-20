package com.starburst.starburst.models.translator.resolvers

import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.cells.Cell

data class CustomDriver(
    val formula: String = "0"
)

class CustomExpressionResolver(private val ctx: ResolverContext): ExpressionResolver {
    override fun resolveExpression(cell: Cell): Cell {
        val expression = cell.driver?.customDriver?.formula ?: error("formula is required for custom cells")
        return StringExpressionResolver(ctx)
            .resolveExpression(cell.copy(expression = expression))
    }
}
