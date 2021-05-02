package com.bdozer.api.web.models.translator.subtypes

import com.bdozer.api.web.models.translator.FormulaTranslationContext
import com.bdozer.api.web.spreadsheet.Cell
import java.time.LocalDate

/**
 * [ManualProjectionsTranslator]
 */
class ManualProjectionsTranslator(val ctx: FormulaTranslationContext) : FormulaTranslator {

    override fun translateFormula(cell: Cell): Cell {
        val currentPeriod = cell.period
        val item = cell.item
        val fy0FiscalYear = item.historicalValue?.documentFiscalYearFocus ?: LocalDate.now().year
        val fiscalYear = fy0FiscalYear + currentPeriod
        val manualProjections = item.manualProjections ?: error("manualProjections must be specified")
        if (manualProjections.manualProjections.isEmpty()) {
            error("manualProjections must not be empty for item ${item.name}")
        }
        val manualProjection = manualProjections.manualProjections.find { it.fiscalYear == fiscalYear }

        return if (manualProjection != null) {
            cell.copy(formula = manualProjection.value.toString())
        } else {
            /*
            if we cannot find a manual projection (i.e. outside the projection period)
            but we still have periods left in the model to project, then decay toward
            terminal growth rate via interpolation
             */
            cell.copy(
                formula = interpolate(cell)
            )
        }
    }

    private fun interpolate(
        cell: Cell,
    ): String {
        val modelPeriods = ctx.model.periods
        val currentPeriod = cell.period
        val item = cell.item
        val manualProjections = item.manualProjections!!
        val fy0FiscalYear = item.historicalValue?.documentFiscalYearFocus ?: LocalDate.now().year
        val lastModelFiscalYear = modelPeriods + fy0FiscalYear

        /*
        if there are more periods than there are projections
        fill the rest with a linearly decreasing growth rate that match the long-term growth rate
         */
        val terminalGrowthRate = ctx.model.terminalGrowthRate
        val lastValue = manualProjections.manualProjections.maxByOrNull { it.fiscalYear } ?: error("...")
        val lastFyWithProjection = lastValue.fiscalYear
        val lastPeriodWithProjection = lastFyWithProjection - fy0FiscalYear
        val numUnprojectedYears = lastModelFiscalYear - lastFyWithProjection

        /*
        find the most recent growth rate in the projected period
         */
        val lastTwoEntries = manualProjections.manualProjections.sortedByDescending { it.fiscalYear }.take(2)
        val latestValue = lastTwoEntries.first().value
        val lastGrowth = if (lastTwoEntries.size != 2) {
            0.0
        } else {
            latestValue / lastTwoEntries.last().value - 1.0
        }

        /*
        compute the slope
         */
        val slope = (lastGrowth - terminalGrowthRate) / numUnprojectedYears
        val growthRates =
            (1..(currentPeriod - lastPeriodWithProjection)).map { period -> (lastGrowth - (slope * period)) }

        /*
        compound the growth rates out
         */
        return growthRates.fold(latestValue) { acc, growthRate -> acc * (growthRate + 1) }.toString()
    }

}