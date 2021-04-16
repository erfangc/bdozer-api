package com.bdozer.models.translator.subtypes

import com.bdozer.models.translator.FormulaTranslationContext
import com.bdozer.spreadsheet.Cell

class PercentOfRevenueTranslator(private val ctx: FormulaTranslationContext) : FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val period = cell.period
        val model = ctx.model
        val item = cell.item

        val percentOfRevenue = item.percentOfRevenue?.percentOfRevenue ?: 0.0
        val totalRevenueConceptName = model.totalRevenueConceptName
        val revenueCellName = "${totalRevenueConceptName}_Period$period"

        return cell.copy(
            formula = "$revenueCellName * $percentOfRevenue",
            dependentCellNames = listOf(revenueCellName)
        )
    }
}
