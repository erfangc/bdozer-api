package com.bdozer.models.translator.subtypes

import com.bdozer.models.translator.FormulaTranslationContext
import com.bdozer.spreadsheet.Cell
import java.time.LocalDate

class DiscreteTranslator(val ctx: FormulaTranslationContext) : FormulaTranslator {

    override fun translateFormula(cell: Cell): Cell {
        val period = cell.period
        val item = cell.item
        val itemName = item.name
        val discrete = item.discrete ?: error("discrete must be specified")
        // if a formula cannot be found for this period, use the previous period's values
        val documentPeriodEndDate = cell.item.historicalValue?.documentPeriodEndDate
        return if (documentPeriodEndDate == null) {
            val formula = discrete.formulas[period] ?: "${itemName}_Period${period}"
            cell.copy(
                formula = formula
            )
        } else {
            val year = LocalDate.parse(documentPeriodEndDate).year + period
            val formula = discrete.formulas[year] ?: "${itemName}_Period${period - 1}"
            cell.copy(
                formula = formula
            )
        }

    }

}