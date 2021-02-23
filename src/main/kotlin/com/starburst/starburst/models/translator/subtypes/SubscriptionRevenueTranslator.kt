package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.spreadsheet.Cell

/**
 * [SubscriptionRevenueTranslator] takes assumptions from [SaaSRevenue] and
 * turns them into expressions that expects growth toward a terminal subscription rate
 */
class SubscriptionRevenueTranslator(private val ctx: ResolverContext) {
    fun resolveExpression(cell: Cell): Cell {
        val periods = ctx.model.periods
        val period = cell.period
        val item = cell.item
        val saaSRevenue = item.subscriptionRevenue ?: error("subscriptionRevenue fields must be populated")

        val (
            totalSubscriptionAtTerminalYear,
            initialSubscriptions,
            averageRevenuePerSubscription
        ) = saaSRevenue

        return cell.copy(
            formula = "((((${totalSubscriptionAtTerminalYear} - ${initialSubscriptions}) / $periods) * $period) + $initialSubscriptions) * $averageRevenuePerSubscription",
            dependentCellNames = emptyList()
        )
    }
}
