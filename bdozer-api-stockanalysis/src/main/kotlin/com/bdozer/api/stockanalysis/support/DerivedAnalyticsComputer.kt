package com.bdozer.api.stockanalysis.support

import bdozer.api.common.extensions.DoubleExtensions.orZero
import com.bdozer.api.models.dataclasses.EvaluateModelResult
import com.bdozer.api.models.dataclasses.Item
import com.bdozer.api.models.dataclasses.Model
import com.bdozer.api.models.dataclasses.Utility.TerminalValuePerShare
import com.bdozer.api.models.dataclasses.spreadsheet.Cell
import com.bdozer.api.models.irr.IRRCalculator
import com.bdozer.api.stockanalysis.dataclasses.DerivedStockAnalytics
import com.bdozer.api.stockanalysis.dataclasses.Waterfall
import com.bdozer.api.stockanalysis.iex.IEXService
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

class DerivedAnalyticsComputer(private val iexService: IEXService) {

    /**
     * Compute derived analytics given the [EvaluateModelResult] from running the stock analyzer
     */
    fun computeDerivedAnalytics(evaluateModelResult: EvaluateModelResult): DerivedStockAnalytics {
        val model = evaluateModelResult.model

        /*
        perform validation
         */
        val profitPerShare = model.allItems().find { it.name == model.epsConceptName }
            ?: error("There must be an Item named ${model.epsConceptName} in your model")

        val shareOutstanding = model.allItems().find { it.name == model.sharesOutstandingConceptName }
            ?: error("There must be an Item named ${model.sharesOutstandingConceptName} in your model")

        val currentPrice = currentPrice(model).orZero()
        /*
        compute IRR
         */
        val epsConceptName = model.epsConceptName
        val cells = evaluateModelResult.cells
        val terminalValues = cells.filter { it.item.name == TerminalValuePerShare }.associateBy { it.period }
        val irr = IRRCalculator.irr(
            income = doubleArrayOf(
                -currentPrice,
                *cells.filter { it.item.name == epsConceptName }.map { cell ->
                    val terminalValue = terminalValues[cell.period]?.value.orZero()
                    cell.value.orZero() + terminalValue
                }.toDoubleArray()
            )
        )

        val finalPrice = evaluateModelResult
            .cells
            .find { cell -> cell.period == model.periods && cell.item.name == TerminalValuePerShare }
            ?.value

        /*
        End of IRR compute
         */
        return DerivedStockAnalytics(
            profitPerShare = profitPerShare,
            shareOutstanding = shareOutstanding,
            businessWaterfall = businessWaterfall(evaluateModelResult),
            currentPrice = currentPrice,
            discountRate = discountRate(evaluateModelResult),
            revenueCAGR = revenueCAGR(evaluateModelResult),
            targetPrice = evaluateModelResult.targetPrice,
            irr = irr,
            finalPrice = finalPrice,
        )
    }

    private fun currentPrice(model: Model): Double? {
        return model.ticker?.let { ticker ->
            iexService.price(ticker)
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
    fun businessWaterfall(evaluateModelResult: EvaluateModelResult): Map<Int, Waterfall> {
        val model = evaluateModelResult.model
        val lookup = model.allItems().associateBy { it.name }
        val revenueItem = model.item(model.totalRevenueConceptName)
        val netIncomeItem = model.item(model.netIncomeConceptName)

        /*
        Travel from netIncomeItem -> revenueItem
         */
        val expenseItems = hashSetOf<Item>()
        val queue = LinkedList<Item>()
        queue.addAll(netIncomeItem?.sumOfOtherItems?.components?.mapNotNull { lookup[it.itemName] } ?: emptyList())
        var latest: Item?

        while (queue.isNotEmpty()) {
            latest = queue.remove()
            val sumOfOtherItems = latest.sumOfOtherItems
            when {
                latest == revenueItem -> {
                    // do nothing
                }
                sumOfOtherItems != null -> {
                    queue.addAll(sumOfOtherItems.components.mapNotNull { lookup[it.itemName] })
                }
                else -> {
                    expenseItems.add(latest)
                }
            }
        }

        return evaluateModelResult
            .cells
            .groupBy { it.period }
            .map { (period, cells) ->
                /*
                process the business waterfall for this period
                 */
                val cellLookupByItemName = cells.associateBy { it.item.name }

                /*
                find total revenue revenue
                 */
                val revenue = cellLookupByItemName[model.totalRevenueConceptName]
                    ?: error("no revenue cell found for period $period")
                val netIncome = cellLookupByItemName[model.netIncomeConceptName]
                    ?: error("no revenue cell found for period $period")
                val expenses = expenseItems.mapNotNull { cellLookupByItemName[it.name] }

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
                        item = Item(name = "Others"),
                    )
                } else {
                    expenses
                }

                /*
                profit
                 */
                val profit =
                    cellLookupByItemName[model.netIncomeConceptName]
                        ?: error("no revenue cell found for period $period")

                period to Waterfall(
                    revenue = revenue,
                    expenses = condensedExpenses,
                    profit = profit,
                )

            }.toMap()
    }
}
