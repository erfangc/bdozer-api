package com.bdozer.stockanalyzer.analyzers.extensions

import com.bdozer.alphavantage.AlphaVantageService
import com.bdozer.extensions.DoubleExtensions.orZero
import com.bdozer.models.EvaluateModelResult
import com.bdozer.models.dataclasses.Item
import com.bdozer.models.dataclasses.ItemType
import com.bdozer.models.dataclasses.Model
import com.bdozer.stockanalyzer.analyzers.StockAnalyzer
import com.bdozer.stockanalyzer.analyzers.extensions.BusinessWaterfall.businessWaterfall
import com.bdozer.stockanalyzer.analyzers.extensions.PostEvaluationAnalysis.allItems
import com.bdozer.stockanalyzer.dataclasses.DerivedStockAnalytics
import com.bdozer.stockanalyzer.dataclasses.Waterfall
import kotlin.math.pow

class PostEvaluationAnalyzer(private val alphaVantageService: AlphaVantageService) {

    fun computeDerivedAnalytics(evaluateModelResult: EvaluateModelResult): DerivedStockAnalytics {
        val model = evaluateModelResult.model

        val profitPerShare = model.allItems().find { it.name == model.epsConceptName }
            ?: error("Cannot find item with name ${model.epsConceptName}")

        val shareOutstanding = model.allItems().find { it.name == model.sharesOutstandingConceptName }
            ?: error("Cannot find item with name ${model.sharesOutstandingConceptName}")

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

    private fun businessWaterfall(evaluateModelResult: EvaluateModelResult): Map<Int, Waterfall> {
        // FIXME work out a robust way to derive
        return emptyMap()
    }
}

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

    fun Model.allItems(): List<Item> {
        return incomeStatementItems + cashFlowStatementItems + balanceSheetItems + otherItems
    }

    private fun StockAnalyzer.profitPerShare(model: Model): Item {
        return model.allItems().find { it.name == epsItemName }
            ?: error("Cannot find item with name $epsItemName")
    }

}