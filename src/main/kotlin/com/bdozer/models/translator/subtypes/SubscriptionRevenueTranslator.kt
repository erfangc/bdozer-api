package com.bdozer.models.translator.subtypes

import com.bdozer.models.translator.FormulaTranslationContext
import com.bdozer.spreadsheet.Cell

/**
 * [SubscriptionRevenueTranslator] takes assumptions from [SaaSRevenue] and
 * turns them into expressions that expects growth toward a terminal subscription rate
 */
class SubscriptionRevenueTranslator(private val ctx: FormulaTranslationContext) {
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