package com.bdozer.models.translator.subtypes

import com.bdozer.models.translator.FormulaTranslationContext
import com.bdozer.spreadsheet.Cell

class DiscreteTranslator(val ctx: FormulaTranslationContext) : FormulaTranslator {

    override fun translateFormula(cell: Cell): Cell {
        val period = cell.period
        val item = cell.item
        val itemName = item.name
        val discrete = item.discrete ?: error("discrete must be specified")
        // if a formula cannot be found for this period, use the previous period's values
        val formula = discrete.formulas[period] ?: "${itemName}_Period${period - 1}"
        return cell.copy(
            formula = formula
        )
    }

}