package com.starburst.starburst.computers.expression.resolvers

import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.models.Cell

data class VariableCost(
    val percentOfRevenue: Double
)

class VariableCostExpressionResolver(private val ctx: ResolverContext): ExpressionResolver {
    override fun resolveExpression(cell: Cell): Cell {
        val period = cell.period
        val percentOfRevenue = cell.driver?.variableCost?.percentOfRevenue ?: 0.0
        // TODO figure out how to define revenue cell name instead of hard coding it here
        val revenueCellName = "Revenue_Period$period"
        return cell.copy(
            expression = "$revenueCellName * $percentOfRevenue",
            dependentCellNames = listOf(revenueCellName)
        )
    }
}
