package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.models.translator.FormulaTranslationContext
import com.starburst.starburst.spreadsheet.Cell

class CompoundedGrowthTranslator(private val ctx: FormulaTranslationContext) : FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val period = cell.period
        val item = cell.item
        val percentOfRevenue = 1 + (item.compoundedGrowth?.growthRate ?: 0.0)
        return cell.copy(
            formula = "${item.name}_Period${period - 1} * $percentOfRevenue",
        )
    }
}
