package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.computers.ReservedItemNames.Revenue
import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.spreadsheet.Cell

data class VariableCost(
    val percentOfRevenue: Double
)

class VariableCostExpressionTranslator(private val ctx: ResolverContext) : ExpressionTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val period = cell.period
        val percentOfRevenue = cell.driver?.variableCost?.percentOfRevenue ?: 0.0
        val revenueCellName = "${Revenue}_Period$period"
        return cell.copy(
            formula = "$revenueCellName * $percentOfRevenue",
            dependentCellNames = listOf(revenueCellName)
        )
    }
}
