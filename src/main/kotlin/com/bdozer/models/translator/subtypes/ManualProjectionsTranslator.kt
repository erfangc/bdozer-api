package com.bdozer.models.translator.subtypes

import com.bdozer.models.dataclasses.ManualProjections
import com.bdozer.models.translator.FormulaTranslationContext
import com.bdozer.spreadsheet.Cell

/**
 * [ManualProjectionsTranslator]
 */
class ManualProjectionsTranslator(val ctx: FormulaTranslationContext) : FormulaTranslator  {

    override fun translateFormula(cell: Cell): Cell {
        val currentPeriod = cell.period
        val item = cell.item
        val manualProjections = item.manualProjections ?: error("manualProjections must be specified")
        val lookup = manualProjections.manualProjections.associateBy { it.period }
        val manualProjection = lookup[currentPeriod]

        return if (manualProjection != null) {
            cell.copy(
                formula = manualProjection.value.toString()
            )
        } else {
            cell.copy(
                formula = interpolate(
                    manualProjections = manualProjections,
                    modelPeriods = ctx.model.periods,
                    currentPeriod = currentPeriod,
                )
            )
        }
    }

    private fun interpolate(
        manualProjections: ManualProjections,
        modelPeriods: Int,
        currentPeriod: Int
    ): String {
        /*
        if there are more periods than there are projections
        fill the rest with a linearly decreasing growth rate that match the long-term growth rate
         */
        val terminalGrowthRate = ctx.model.terminalGrowthRate
        val lastValue = manualProjections.manualProjections.maxByOrNull { it.period } ?: error("...")
        val maxProjectionPeriod = lastValue.period
        val periodDifference = modelPeriods - maxProjectionPeriod

        /*
        find the most recent growth rate in the projected period
         */
        val lastTwoEntries = manualProjections.manualProjections.sortedByDescending { it.period }.take(2)
        val latestValue = lastTwoEntries.first().value
        val lastGrowth = if (lastTwoEntries.size != 2) {
            0.0
        } else {
            latestValue / lastTwoEntries.last().value - 1.0
        }

        /*
        compute the slope
         */
        val slope = (lastGrowth - terminalGrowthRate) / periodDifference
        val growthRates = (1..(currentPeriod - maxProjectionPeriod)).map { period -> (lastGrowth - (slope * period)) }

        /*
        compound the growth rates out
         */
        return growthRates.fold(latestValue) { acc, growthRate -> acc * (growthRate + 1) }.toString()
    }

}