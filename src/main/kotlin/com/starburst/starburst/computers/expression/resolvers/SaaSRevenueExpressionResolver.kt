package com.starburst.starburst.computers.expression.resolvers

import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.models.Cell

data class SaaSRevenue(
    val totalSubscriptionAtTerminalYear: Int,
    val initialSubscriptions: Int,
    val averageRevenuePerSubscription: Double
)

/**
 * [SaaSRevenueExpressionResolver] takes assumptions from [SaaSRevenue] and
 * turns them into expressions that expects growth toward a terminal subscription rate
 */
class SaaSRevenueExpressionResolver(private val ctx: ResolverContext) {
    fun resolveExpression(cell: Cell): Cell {
        val periods = ctx.model.periods
        val period = cell.period
        val driver = cell.driver
        val saaSRevenue = driver?.saaSRevenue ?: error("saaSRevenue fields must be populated")

        val (
            totalSubscriptionAtTerminalYear,
            initialSubscriptions,
            averageRevenuePerSubscription
        ) = saaSRevenue

        return cell.copy(
            expression = "((((${totalSubscriptionAtTerminalYear} - ${initialSubscriptions}) / $periods) * $period) + $initialSubscriptions) * $averageRevenuePerSubscription",
            dependentCellNames = emptyList()
        )
    }
}
