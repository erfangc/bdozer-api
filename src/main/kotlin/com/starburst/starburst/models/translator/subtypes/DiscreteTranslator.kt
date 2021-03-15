package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.models.translator.FormulaTranslationContext
import com.starburst.starburst.spreadsheet.Cell

class DiscreteTranslator(val ctx: FormulaTranslationContext): FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val period = cell.period
        val discrete = cell.item.discrete ?: error("discrete must be specified")
        val formula = discrete
            .formulas[period] ?: error("unable to find a value for period $period on discrete item formula")
        return cell.copy(
            formula = formula
        )
    }
}