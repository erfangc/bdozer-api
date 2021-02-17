package com.starburst.starburst.computers.expression.resolvers

import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.models.Cell

data class FixedCost(
    val cost: Double
)

class FixedCostExpressionResolver(private val ctx: ResolverContext): ExpressionResolver {
    override fun resolveExpression(cell: Cell): Cell {
        val driver = cell.driver
        val fixedCost = driver?.fixedCost ?: error("")
        return cell.copy(
            expression = "${fixedCost.cost}",
            dependentCellNames = emptyList()
        )
    }
}
