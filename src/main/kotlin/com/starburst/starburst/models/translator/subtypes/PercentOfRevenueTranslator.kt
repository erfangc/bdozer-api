package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.models.translator.FormulaTranslationContext
import com.starburst.starburst.spreadsheet.Cell

class PercentOfRevenueTranslator(private val ctx: FormulaTranslationContext) : FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val period = cell.period
        val percentOfRevenue = cell.item.percentOfRevenue?.percentOfRevenue ?: 0.0
        val revenueCellName = "${ctx.model.totalRevenueConceptName}_Period$period"
        return cell.copy(
            formula = "$revenueCellName * $percentOfRevenue",
            dependentCellNames = listOf(revenueCellName)
        )
    }
}
