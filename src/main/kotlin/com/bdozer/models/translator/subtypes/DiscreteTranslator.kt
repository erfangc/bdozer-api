package com.bdozer.models.translator.subtypes

import com.bdozer.models.dataclasses.Discrete
import com.bdozer.models.translator.FormulaTranslationContext
import com.bdozer.spreadsheet.Cell
import java.time.LocalDate

class DiscreteTranslator(val ctx: FormulaTranslationContext) : FormulaTranslator {

    override fun translateFormula(cell: Cell): Cell {
        val currentPeriod = cell.period
        val item = cell.item
        val itemName = item.name
        val discrete = item.discrete ?: error("discrete must be specified")
        // if a formula cannot be found for this period, use the previous period's values
        val documentPeriodEndDate = cell.item.historicalValue?.documentPeriodEndDate

        return if (documentPeriodEndDate == null) {
            val formula = discrete.formulas[currentPeriod] ?: interpolate(discrete, currentPeriod) ?: "${itemName}_Period${currentPeriod}"
            cell.copy(formula = formula)
        } else {
            val year = LocalDate.parse(documentPeriodEndDate).year + currentPeriod
            val formula = discrete.formulas[year] ?: interpolate(discrete, currentPeriod) ?: "${itemName}_Period${currentPeriod - 1}"

            cell.copy(
                formula = formula
            )
        }

    }

    private fun interpolate(discrete: Discrete, currentPeriod: Int): String? {
        // if there are more periods than there are projections
        // fill the rest with a linearly decreasing growth rate that match the long-term growth rate
        val modelPeriods = ctx.model.periods
        val terminalGrowthRate = ctx.model.terminalGrowthRate
        val maxProjectionPeriod = discrete.formulas.keys.maxOrNull() ?: modelPeriods
        val periodDifference = modelPeriods - maxProjectionPeriod
        // find the most recent growth rate in the projected period
        val lastTwoEntries = discrete.formulas.entries.sortedByDescending { it.key }.take(2)
        val lastGrowth = if (lastTwoEntries.size != 2) {
            0.0
        } else {
            lastTwoEntries.first().value.toDouble() / lastTwoEntries.last().value.toDouble() - 1.0
        }
        val lastProjection = lastTwoEntries.first().value.toDouble()

        // compute the slope
        val slope = (lastGrowth - terminalGrowthRate) / periodDifference
        val growthRates = (1..(currentPeriod - maxProjectionPeriod)).map { period -> (lastGrowth - (slope * period)) }
        // compound the growth rates out
        return growthRates.fold(lastProjection) { acc, growthRate -> acc * (growthRate + 1) }.toString()
    }

}