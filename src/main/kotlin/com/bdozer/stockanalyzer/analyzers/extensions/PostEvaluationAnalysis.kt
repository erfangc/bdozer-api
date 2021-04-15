package com.bdozer.stockanalyzer.analyzers.extensions

import com.bdozer.extensions.DoubleExtensions.orZero
import com.bdozer.models.EvaluateModelResult
import com.bdozer.models.dataclasses.Item
import com.bdozer.models.dataclasses.ItemType
import com.bdozer.models.dataclasses.Model
import com.bdozer.stockanalyzer.analyzers.AbstractStockAnalyzer
import com.bdozer.stockanalyzer.analyzers.extensions.BusinessWaterfall.businessWaterfall
import com.bdozer.stockanalyzer.dataclasses.DerivedStockAnalytics
import com.bdozer.stockanalyzer.dataclasses.StockAnalysis2
import java.util.concurrent.Future
import kotlin.math.pow

object PostEvaluationAnalysis {

    fun AbstractStockAnalyzer.postModelEvaluationAnalysis(
        evaluateModelResultFuture: Future<EvaluateModelResult>,
        zeroRevenueGrowthResultFuture: Future<EvaluateModelResult>
    ): StockAnalysis2 {
        val evaluateModelResult = evaluateModelResultFuture.get()
        val zeroRevenueGrowthResult = zeroRevenueGrowthResultFuture.get()
        val model = evaluateModelResult.model
        return originalStockAnalysis.copy(
            cik = cik,
            name = filingEntity.name,
            ticker = filingEntity.tradingSymbol,
            model = evaluateModelResult.model.copy(
                totalRevenueConceptName = totalRevenueConceptName,
                epsConceptName = epsConceptName,
                netIncomeConceptName = netIncomeConceptName,
                ebitConceptName = ebitConceptName,
                operatingCostConceptName = operatingCostConceptName,
                sharesOutstandingConceptName = sharesOutstandingConceptName,
            ),
            cells = evaluateModelResult.cells,
            derivedStockAnalytics = DerivedStockAnalytics(
                profitPerShare = profitPerShare(model),
                shareOutstanding = shareOutstanding(model),
                businessWaterfall = businessWaterfall(evaluateModelResult),
                zeroGrowthPrice = zeroRevenueGrowthResult.targetPrice,
                targetPrice = evaluateModelResult.targetPrice,
                discountRate = (model.equityRiskPremium * model.beta) + model.riskFreeRate,
                revenueCAGR = revenueCAGR(evaluateModelResult),
                currentPrice = currentPrice(filingEntity.tradingSymbol),
            ),
        )

    }

    private fun AbstractStockAnalyzer.currentPrice(tradingSymbol: String?): Double {
        return alphaVantageService.latestPrice(tradingSymbol ?: error("..."))
    }

    public fun AbstractStockAnalyzer.zeroRevenueGrowth(model: Model): Model {
        val incomeStatementItems = model.incomeStatementItems.map { item ->
            if (item.name == totalRevenueConceptName) {
                item.copy(formula = item.historicalValue?.value.toString(), type = ItemType.Custom)
            } else {
                item
            }
        }
        return model.copy(incomeStatementItems = incomeStatementItems)
    }

    private fun AbstractStockAnalyzer.revenueCAGR(evalResult: EvaluateModelResult): Double {
        val revenues = evalResult
            .cells
            .filter { cell -> cell.item.name == totalRevenueConceptName }
        return (revenues.last().value.orZero() / revenues.first().value.orZero()).pow(1.0 / revenues.size) - 1
    }

    private fun Model.allItems(): List<Item> {
        return incomeStatementItems + cashFlowStatementItems + balanceSheetItems + otherItems
    }

    private fun AbstractStockAnalyzer.shareOutstanding(model: Model): Item {
        return model.allItems().find { it.name == sharesOutstandingConceptName }
            ?: error("Cannot find item with name $sharesOutstandingConceptName")
    }

    private fun AbstractStockAnalyzer.profitPerShare(model: Model): Item {
        return model.allItems().find { it.name == epsConceptName }
            ?: error("Cannot find item with name $epsConceptName")
    }

}