package com.starburst.starburst.stockanalyzer.analyzers.extensions

import com.starburst.starburst.extensions.DoubleExtensions.orZero
import com.starburst.starburst.models.EvaluateModelResult
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.ItemType
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.stockanalyzer.analyzers.AbstractStockAnalyzer
import com.starburst.starburst.stockanalyzer.analyzers.extensions.BusinessWaterfall.businessWaterfall
import com.starburst.starburst.stockanalyzer.staging.dataclasses.DerivedStockAnalytics
import com.starburst.starburst.stockanalyzer.staging.dataclasses.StockAnalysis2
import kotlin.math.pow

object PostEvaluationAnalysis {

    fun AbstractStockAnalyzer.postModelEvaluationAnalysis(evalResult: EvaluateModelResult): StockAnalysis2 {
        val model = evalResult.model
        /*
        try a version of this where revenue remains constant
         */
        val zeroGrowthResult = evaluator.evaluate(zeroRevenueGrowth(model))
        val zeroGrowthPrice = zeroGrowthResult.targetPrice.coerceAtLeast(0.0)

        return originalStockAnalysis.copy(
            cik = cik,
            name = filingEntity.name,
            ticker = filingEntity.tradingSymbol,
            model = evalResult.model.copy(
                totalRevenueConceptName = totalRevenueConceptName,
                epsConceptName = epsConceptName,
                netIncomeConceptName = netIncomeConceptName,
                ebitConceptName = ebitConceptName,
                operatingCostConceptName = operatingCostConceptName,
                sharesOutstandingConceptName = sharesOutstandingConceptName,
            ),
            cells = evalResult.cells,
            derivedStockAnalytics = DerivedStockAnalytics(
                profitPerShare = profitPerShare(model),
                shareOutstanding = shareOutstanding(model),
                businessWaterfall = businessWaterfall(evalResult),
                zeroGrowthPrice = zeroGrowthPrice,
                targetPrice = evalResult.targetPrice,
                discountRate = (model.equityRiskPremium * model.beta) + model.riskFreeRate,
                revenueCAGR = revenueCAGR(evalResult),
                currentPrice = currentPrice(filingEntity.tradingSymbol),
            ),
        )

    }

    private fun AbstractStockAnalyzer.currentPrice(tradingSymbol: String?): Double {
        return alphaVantageService.latestPrice(tradingSymbol ?: error("..."))
    }

    private fun AbstractStockAnalyzer.zeroRevenueGrowth(model: Model): Model {
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