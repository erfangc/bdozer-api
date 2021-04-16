package com.bdozer.stockanalyzer.analyzers.extensions

import com.bdozer.extensions.DoubleExtensions.orZero
import com.bdozer.models.EvaluateModelResult
import com.bdozer.models.dataclasses.Item
import com.bdozer.models.dataclasses.ItemType
import com.bdozer.models.dataclasses.Model
import com.bdozer.stockanalyzer.analyzers.StockAnalyzer
import com.bdozer.stockanalyzer.analyzers.extensions.BusinessWaterfall.businessWaterfall
import com.bdozer.stockanalyzer.dataclasses.DerivedStockAnalytics
import com.bdozer.stockanalyzer.dataclasses.StockAnalysis2
import java.util.concurrent.Future
import kotlin.math.pow

object PostEvaluationAnalysis {

    fun StockAnalyzer.runDerivedAnalytics(
        evaluateModelResultFuture: EvaluateModelResult,
        zeroRevenueGrowthResultFuture: EvaluateModelResult,
    ): DerivedStockAnalytics {
        val evaluateModelResult = evaluateModelResultFuture
        val zeroRevenueGrowthResult = zeroRevenueGrowthResultFuture
        val model = evaluateModelResult.model
        return DerivedStockAnalytics(
            profitPerShare = profitPerShare(model),
            shareOutstanding = sharesOutstandingItem ?: error("..."),
            businessWaterfall = businessWaterfall(evaluateModelResult),
            zeroGrowthPrice = zeroRevenueGrowthResult.targetPrice,
            targetPrice = evaluateModelResult.targetPrice,
            discountRate = (model.equityRiskPremium * model.beta) + model.riskFreeRate,
            revenueCAGR = revenueCAGR(evaluateModelResult),
            currentPrice = currentPrice(filingEntity.tradingSymbol),
        )
    }

    private fun StockAnalyzer.currentPrice(tradingSymbol: String?): Double {
        return alphaVantageService.latestPrice(tradingSymbol ?: error("..."))
    }

    fun StockAnalyzer.zeroRevenueGrowth(model: Model): Model {
        val incomeStatementItems = model.incomeStatementItems.map { item ->
            if (item.name == totalRevenueItemName) {
                item.copy(formula = item.historicalValue?.value.toString(), type = ItemType.Custom)
            } else {
                item
            }
        }
        return model.copy(incomeStatementItems = incomeStatementItems)
    }

    private fun StockAnalyzer.revenueCAGR(evalResult: EvaluateModelResult): Double {
        val revenues = evalResult
            .cells
            .filter { cell -> cell.item.name == totalRevenueItemName }
        return (revenues.last().value.orZero() / revenues.first().value.orZero()).pow(1.0 / revenues.size) - 1
    }

    private fun Model.allItems(): List<Item> {
        return incomeStatementItems + cashFlowStatementItems + balanceSheetItems + otherItems
    }

    private fun StockAnalyzer.profitPerShare(model: Model): Item {
        return model.allItems().find { it.name == epsItemName }
            ?: error("Cannot find item with name $epsItemName")
    }

}