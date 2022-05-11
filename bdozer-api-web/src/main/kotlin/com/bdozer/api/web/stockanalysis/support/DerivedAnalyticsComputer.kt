package com.bdozer.api.web.stockanalysis.support

import com.bdozer.api.models.dataclasses.EvaluateModelResult
import com.bdozer.api.models.dataclasses.Item
import com.bdozer.api.models.dataclasses.Model
import com.bdozer.api.models.dataclasses.Utility
import com.bdozer.api.models.dataclasses.Utility.TerminalValuePerShare
import com.bdozer.api.models.dataclasses.spreadsheet.Cell
import com.bdozer.api.stockanalysis.models.DerivedStockAnalytics
import com.bdozer.api.stockanalysis.models.Waterfall
import com.bdozer.api.stockanalysis.support.IRRCalculator.irr
import com.bdozer.api.web.stockanalysis.support.poylgon.PolygonService
import org.springframework.stereotype.Service
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

@Service
class DerivedAnalyticsComputer(private val polygonService: PolygonService) {
    
    private fun Double?.orZero() = this ?: 0.0

    /**
     * Compute derived analytics given the [EvaluateModelResult] from running the stock analyzer
     *
     * @param cells the evaluated cells
     * @param model the model from which the evaluated cells derived from
     */
    fun computeDerivedAnalytics(model: Model, cells: List<Cell>): DerivedStockAnalytics {

        val currentPrice = model.ticker?.let { ticker ->
            val previousClose = polygonService.previousClose(ticker = ticker)
            previousClose.results.firstOrNull()?.c
        }
        /*
        perform validation
         */
        val profitPerShare = model.allItems().find { it.name == model.epsConceptName }
            ?: error("There must be an Item named ${model.epsConceptName} in your model")

        val shareOutstanding = model.allItems().find { it.name == model.sharesOutstandingConceptName }
            ?: error("There must be an Item named ${model.sharesOutstandingConceptName} in your model")

        /*
        compute the target price as NPV
         */
        val targetPrice = cells
            .filter { cell -> cell.item.name == Utility.PresentValuePerShare }
            .sumOf { cell -> cell.value ?: 0.0 }

        /*
        compute IRR
         */
        val epsConceptName = model.epsConceptName
        val terminalValues = cells.filter { it.item.name == TerminalValuePerShare }.associateBy { it.period }
        val irr = currentPrice?.let {
            irr(
                income = doubleArrayOf(
                    -currentPrice,
                    *cells.filter { it.item.name == epsConceptName }.map { cell ->
                        val terminalValue = terminalValues[cell.period]?.value.orZero()
                        cell.value.orZero() + terminalValue
                    }.toDoubleArray()
                )
            )
        }

        val finalPrice = cells
            .find { cell -> cell.period == model.periods && cell.item.name == TerminalValuePerShare }
            ?.value

        /*
        Create the Derived analytics wrapper object
         */
        return DerivedStockAnalytics(
            profitPerShare = profitPerShare,
            shareOutstanding = shareOutstanding,
            businessWaterfall = businessWaterfall(model, cells),
            currentPrice = currentPrice,
            discountRate = discountRate(model),
            revenueCAGR = revenueCAGR(model, cells),
            targetPrice = targetPrice,
            finalPrice = finalPrice,
            irr = irr,
        )
    }

    private fun revenueCAGR(model: Model, cells: List<Cell>): Double {
        val revenues = cells
            .filter { cell -> cell.item.name == model.totalRevenueConceptName }
        return (revenues.last().value.orZero() / revenues.first().value.orZero()).pow(1.0 / revenues.size) - 1
    }

    private fun discountRate(model: Model) = (model.equityRiskPremium * model.beta) + model.riskFreeRate

    /**
     * This method computes the [Waterfall] for every period. A [Waterfall] groups - for 1 period -
     * the major expenses into at most 5 categories for ease of display
     */
    private fun businessWaterfall(model: Model, cells: List<Cell>): Map<Int, Waterfall> {
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

        return cells
            .groupBy { it.period }
            .map { (period, cells) ->
                /*
                process the business waterfall for this period
                 */
                val cellLookupByItemName = cells.associateBy { it.item.name }

                /*
                find total revenue
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
