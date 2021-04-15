package com.bdozer.stockanalyzer.analyzers.extensions

import com.bdozer.models.EvaluateModelResult
import com.bdozer.models.dataclasses.Item
import com.bdozer.spreadsheet.Cell
import com.bdozer.stockanalyzer.analyzers.AbstractStockAnalyzer
import com.bdozer.stockanalyzer.dataclasses.Waterfall
import kotlin.math.abs

object BusinessWaterfall {

    /**
     * This method computes the [Waterfall] for every period. A [Waterfall] groups - for 1 period -
     * the major expenses into at most 5 categories for ease of display
     */
    fun AbstractStockAnalyzer.businessWaterfall(evalResult: EvaluateModelResult): Map<Int, Waterfall> {
        return evalResult
            .cells
            .groupBy { it.period }
            .map { (period, cells) ->
                val cells = cells.associateBy { it.item.name }
                /*
                Find total revenue revenue
                 */
                val revenue = cells[totalRevenueConceptName] ?: error("no revenue cell found for period $period")

                /*
                Find all the expenses
                 */
                val expenses = conceptDependencies[netIncomeConceptName]
                    ?.filter { calculation -> calculation.conceptName != totalRevenueConceptName }
                    ?.mapNotNull { calculation ->
                        val cell = cells[calculation.conceptName]
                        val concept = conceptManager.getConcept(calculation.conceptHref)
                        val weight = if (concept?.balance == "credit") 1.0 else -1.0
                        cell?.copy(value = (cell.value ?: 0.0) * weight)
                    }
                    ?.sortedByDescending { abs(it.value ?: 0.0) }
                    ?: emptyList()
                val conceptSize = expenses.size
                val cutoff = 5

                /*
                top 5 expenses and then lump everything else into Other
                 */
                val condensedExpenses = if (conceptSize > cutoff) {
                    val top5Expenses = expenses.subList(0, cutoff)
                    val others = expenses.subList(cutoff, expenses.size)
                    top5Expenses + Cell(
                        name = "Others",
                        value = others.sumByDouble { cell -> cell.value ?: 0.0 },
                        period = period,
                        item = Item(name = "Others")
                    )
                } else {
                    expenses
                }

                /*
                profit
                 */
                val profit = cells[netIncomeConceptName] ?: error("no revenue cell found for period $period")

                period to Waterfall(revenue = revenue, expenses = condensedExpenses, profit = profit)
            }.toMap()
    }

}