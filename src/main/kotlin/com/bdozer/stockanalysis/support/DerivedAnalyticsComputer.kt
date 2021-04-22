package com.bdozer.stockanalysis.support

import com.bdozer.alphavantage.AlphaVantageService
import com.bdozer.extensions.DoubleExtensions.orZero
import com.bdozer.models.EvaluateModelResult
import com.bdozer.models.dataclasses.Item
import com.bdozer.models.dataclasses.ItemType
import com.bdozer.models.dataclasses.Model
import com.bdozer.spreadsheet.Cell
import com.bdozer.stockanalysis.dataclasses.DerivedStockAnalytics
import com.bdozer.stockanalysis.dataclasses.Waterfall
import com.bdozer.stockanalysis.support.StatelessModelEvaluator.Companion.allItems
import org.springframework.stereotype.Service
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

@Service
class DerivedAnalyticsComputer(private val alphaVantageService: AlphaVantageService) {

    fun computeDerivedAnalytics(evaluateModelResult: EvaluateModelResult): DerivedStockAnalytics {
        val model = evaluateModelResult.model

        /*
        perform validation
         */
        val profitPerShare = model.allItems().find { it.name == model.epsConceptName }
            ?: error("There must be an Item named ${model.epsConceptName} in your model")

        val shareOutstanding = model.allItems().find { it.name == model.sharesOutstandingConceptName }
            ?: error("There must be an Item named ${model.sharesOutstandingConceptName} in your model")

        return DerivedStockAnalytics(
            profitPerShare = profitPerShare,
            shareOutstanding = shareOutstanding,
            businessWaterfall = businessWaterfall(evaluateModelResult),
            currentPrice = currentPrice(model).orZero(),
            discountRate = discountRate(evaluateModelResult),
            revenueCAGR = revenueCAGR(evaluateModelResult),
            targetPrice = evaluateModelResult.targetPrice,
            // FIXME
            zeroGrowthPrice = 0.0,
        )
    }

    private fun currentPrice(model: Model): Double? {
        return model.ticker?.let { ticker ->
            alphaVantageService.latestPrice(ticker)
        }
    }

    private fun revenueCAGR(evalResult: EvaluateModelResult): Double {
        val model = evalResult.model
        val revenues = evalResult
            .cells
            .filter { cell -> cell.item.name == model.totalRevenueConceptName }
        return (revenues.last().value.orZero() / revenues.first().value.orZero()).pow(1.0 / revenues.size) - 1
    }

    private fun discountRate(evalResult: EvaluateModelResult): Double {
        val model = evalResult.model
        return (model.equityRiskPremium * model.beta) + model.riskFreeRate
    }


    /**
     * This method computes the [Waterfall] for every period. A [Waterfall] groups - for 1 period -
     * the major expenses into at most 5 categories for ease of display
     */
    fun businessWaterfall(evalResult: EvaluateModelResult): Map<Int, Waterfall> {
        val model = evalResult.model
        val totalRevenueItemName = model.totalRevenueConceptName
        val netIncomeItemName = model.netIncomeConceptName
        return evalResult
            .cells
            .groupBy { it.period }
            .map { (period, cells) ->
                /*
                process the business waterfall for this period
                 */

                val cellLookupByItemName = cells.associateBy { it.item.name }
                val cellLookupByCellName = cells.associateBy { it.name }
                /*
                Find total revenue revenue
                 */
                val revenue =
                    cellLookupByItemName[totalRevenueItemName] ?: error("no revenue cell found for period $period")
                val netIncome =
                    cellLookupByItemName[netIncomeItemName] ?: error("no revenue cell found for period $period")

                /*
                Find all the expenses by traversing its dependency tree between
                net income and revenue
                 */
                val visited = hashSetOf<String?>()

                val dependentCellNames = Stack<String>()
                dependentCellNames.addAll(netIncome.dependentCellNames)

                while (dependentCellNames.isNotEmpty()) {
                    val dependentCellName = dependentCellNames.pop()
                    val dependentCell = cellLookupByCellName[dependentCellName]
                    // skip if this is the revenue cell
                    if (dependentCell?.name != revenue.name) {
                        visited.add(dependentCell?.name)
                        val unvisited = dependentCell
                            ?.dependentCellNames
                            ?.filter { !visited.contains(it) }
                            ?: emptyList()
                        dependentCellNames.addAll(unvisited)
                    }
                }

                val expenses = visited
                    .filterNotNull()
                    .map { cellName ->
                        cellLookupByCellName[cellName]!!
                    }
                    .filter { it.item.type != ItemType.SumOfOtherItems }

                val cutoff = 5

                /*
                top 5 expenses and then lump everything else into Other
                 */
                val condensedExpenses = if (expenses.size > cutoff) {
                    val top5Expenses = expenses.sortedByDescending { abs(it.value.orZero()) }.subList(0, cutoff)
                    val sumOfTop5 = top5Expenses.sumByDouble { it.value.orZero() }
                    val plug = revenue.value.orZero() - sumOfTop5 - netIncome.value.orZero()
                    top5Expenses + Cell(
                        name = "Others",
                        value = plug,
                        period = period,
                        item = Item(name = "Others")
                    )
                } else {
                    expenses
                }

                /*
                profit
                 */
                val profit =
                    cellLookupByItemName[netIncomeItemName] ?: error("no revenue cell found for period $period")

                period to Waterfall(revenue = revenue, expenses = condensedExpenses, profit = profit)
            }.toMap()
    }
}
