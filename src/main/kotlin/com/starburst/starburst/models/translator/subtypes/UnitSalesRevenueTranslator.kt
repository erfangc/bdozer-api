package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.models.translator.ResolverContext
import com.starburst.starburst.spreadsheet.Cell

class UnitSalesRevenueTranslator(
    private val ctx: ResolverContext
) : FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val periods = ctx.model.periods
        val period = cell.period
        val item = cell.item
        val unitSalesRevenue = item.unitSalesRevenue ?: error("unitSalesRevenue fields must be populated")

        val (
            steadyStateUnitsSold, averageSellingPrice, initialUnitsSold,
        ) = unitSalesRevenue

        return cell.copy(
            formula = "(((($steadyStateUnitsSold-$initialUnitsSold)/$periods)*$period)+$initialUnitsSold)*$averageSellingPrice",
            dependentCellNames = emptyList()
        )
    }
}
