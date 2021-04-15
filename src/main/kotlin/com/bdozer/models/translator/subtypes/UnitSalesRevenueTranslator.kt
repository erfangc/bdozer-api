package com.bdozer.models.translator.subtypes

import com.bdozer.models.translator.FormulaTranslationContext
import com.bdozer.spreadsheet.Cell

class UnitSalesRevenueTranslator(
    private val ctx: FormulaTranslationContext
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
