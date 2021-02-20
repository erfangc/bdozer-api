package com.starburst.starburst.models.translator.resolvers

import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.cells.Cell

data class FixedCost(
    val cost: Double
)

class FixedCostExpressionResolver(private val ctx: ResolverContext): ExpressionResolver {
    override fun resolveExpression(cell: Cell): Cell {
        val driver = cell.driver
        val fixedCost = driver?.fixedCost ?: error("a fixed cost must be specified")
        return cell.copy(
            expression = "${fixedCost.cost}",
            dependentCellNames = emptyList()
        )
    }
}
