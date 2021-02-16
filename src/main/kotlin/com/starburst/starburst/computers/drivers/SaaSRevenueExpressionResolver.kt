package com.starburst.starburst.computers.drivers

import com.starburst.starburst.models.Cell
import com.starburst.starburst.models.Model

data class SaaSRevenue(
    val totalSubscriptionAtTerminalYear: Int,
    val initialSubscriptions: Int,
    val averageRevenuePerSubscription: Double
)

class SaaSRevenueExpressionResolver {
    fun resolveExpression(model: Model, cell: Cell): Cell {
        val periods = model.periods ?: 5
        val period = cell.period
        val driver = cell.driver
        val saaSRevenue = driver.saaSRevenue ?: error("")

        val (
            totalSubscriptionAtTerminalYear,
            initialSubscriptions,
            averageRevenuePerSubscription
        ) = saaSRevenue

        return cell.copy(
            expression = "((((${totalSubscriptionAtTerminalYear} - ${initialSubscriptions}) / $periods) * $period) + $initialSubscriptions) * $averageRevenuePerSubscription",
            dependencies = emptyList()
        )
    }
}
