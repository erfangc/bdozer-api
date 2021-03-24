package com.starburst.starburst.modelbuilder.common

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.NetIncomeLoss
import com.starburst.starburst.models.EvaluateModelResult
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.spreadsheet.Cell
import java.net.URI

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
                    ?.filter { conceptName -> conceptName != revenueConceptName }
                    ?.mapNotNull { conceptName -> cells[conceptName] } ?: emptyList()
                val conceptSize = expenses.size
                val cutoff = 5

                /*
                top 5
                 */
                expenses.subList(0, cutoff)
                val topExpenses = if (conceptSize > cutoff) {
                    val others = expenses.subList(cutoff, expenses.size)
                    val subList = expenses.subList(0, cutoff)
                    subList + Cell(
                        name = "Others",
                        value = others.sumByDouble { cell -> cell.item.historicalValue?.value ?: 0.0 },
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

                period to Waterfall(revenue = revenue, topExpenses = topExpenses, profit = profit)
            }.toMap()
    }

}