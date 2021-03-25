package com.starburst.starburst.modelbuilder.common

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.NetIncomeLoss
import com.starburst.starburst.models.EvaluateModelResult
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.spreadsheet.Cell
import java.net.URI
import kotlin.math.abs

object Extensions {
    fun String.fragment(): String = URI(this).fragment

    fun AbstractModelBuilder.businessWaterfall(evalResult: EvaluateModelResult): Map<Int, Waterfall> {
        return evalResult
            .cells
            .groupBy { it.period }
            .map { (period, cells) ->
                val cells = cells.associateBy { it.item.name }
                /*
                extract revenue
                 */
                val revenue = cells[revenueConceptName] ?: error("no revenue cell found for period $period")

                /*
                extract expenses
                 */
                val expenses = conceptDependencies[NetIncomeLoss]
                    ?.filter { calculation -> calculation.conceptName != revenueConceptName }
                    ?.mapNotNull { calculation ->
                        val cell = cells[calculation.conceptName]
                        val concept = conceptManager.getConceptDefinition(calculation.conceptHref)
                        val weight = if (concept?.balance == "credit") 1.0 else -1.0
                        cell?.copy(value = (cell.value ?: 0.0) * weight)
                    }
                    ?.sortedByDescending { abs(it.value ?: 0.0) }
                    ?: emptyList()
                val conceptSize = expenses.size
                val cutoff = 5

                /*
                top 5
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
                val profit = cells[NetIncomeLoss] ?: error("no revenue cell found for period $period")

                period to Waterfall(revenue = revenue, topExpenses = condensedExpenses, profit = profit)
            }.toMap()
    }

}