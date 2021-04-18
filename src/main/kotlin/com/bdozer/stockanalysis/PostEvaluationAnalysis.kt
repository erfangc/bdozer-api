package com.bdozer.stockanalysis

import com.bdozer.alphavantage.AlphaVantageService
import com.bdozer.extensions.DoubleExtensions.orZero
import com.bdozer.models.EvaluateModelResult
import com.bdozer.models.dataclasses.Item
import com.bdozer.models.dataclasses.Model
import com.bdozer.spreadsheet.Cell
import com.bdozer.stockanalysis.StatelessModelEvaluator.Companion.allItems
import com.bdozer.stockanalysis.dataclasses.DerivedStockAnalytics
import com.bdozer.stockanalysis.dataclasses.Waterfall
import org.springframework.stereotype.Service
import kotlin.math.pow

@Service
class PostEvaluationAnalyzer(private val alphaVantageService: AlphaVantageService) {

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
                val cellsLookup = cells.associateBy { it.item.name }
                /*
                Find total revenue revenue
                 */
                val revenue = cellsLookup[totalRevenueItemName] ?: error("no revenue cell found for period $period")

                /*
                Find all the expenses
                TODO figure this out
                 */
                val revenueIdx = model.incomeStatementItems.indexOfFirst { it.name == totalRevenueItemName }
                val netIncomeIdx = model.incomeStatementItems.indexOfFirst { it.name == netIncomeItemName }
                val expenses = cells.subList(revenueIdx + 1, netIncomeIdx)
                val cutoff = 5

                /*
                top 5 expenses and then lump everything else into Other
                 */
                val condensedExpenses = if (expenses.size > cutoff) {
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
                val profit = cellsLookup[netIncomeItemName] ?: error("no revenue cell found for period $period")

                period to Waterfall(revenue = revenue, expenses = condensedExpenses, profit = profit)
            }.toMap()
    }
}
