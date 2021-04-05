package com.starburst.starburst.stockanalyzer.common.extensions

import com.starburst.starburst.DoubleExtensions.orZero
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareDiluted
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.WeightedAverageNumberOfDilutedSharesOutstanding
import com.starburst.starburst.stockanalyzer.common.AbstractStockAnalyzer
import com.starburst.starburst.stockanalyzer.common.extensions.BusinessWaterfall.businessWaterfall
import com.starburst.starburst.stockanalyzer.dataclasses.StockAnalysis
import com.starburst.starburst.models.EvaluateModelResult
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.ItemType
import com.starburst.starburst.models.dataclasses.Model
import kotlin.math.pow

object PostEvaluationAnalysis {

    fun AbstractStockAnalyzer.postModelEvaluationAnalysis(evalResult: EvaluateModelResult): StockAnalysis {
        val model = evalResult.model
        /*
        try a version of this where revenue remains constant
         */
        val zeroGrowthResult = evaluator.evaluate(zeroRevenueGrowth(model))
        val zeroGrowthPrice = zeroGrowthResult.targetPrice.coerceAtLeast(0.0)

        return StockAnalysis(
            _id = cik,
            cik = cik,
            ticker = filingEntity.tradingSymbol,
            model = evalResult.model,
            cells = evalResult.cells,
            profitPerShare = profitPerShare(model),
            shareOutstanding = shareOutstanding(model),
            businessWaterfall = businessWaterfall(evalResult),
            zeroGrowthPrice = zeroGrowthPrice,
            targetPrice = evalResult.targetPrice,
            discountRate = (model.equityRiskPremium * model.beta) + model.riskFreeRate,
            revenueCAGR = revenueCAGR(evalResult),
        )

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

    private fun shareOutstanding(model: Model): Item {
        return model.allItems().find { it.name == WeightedAverageNumberOfDilutedSharesOutstanding }
            ?: error("Cannot find item with name $WeightedAverageNumberOfDilutedSharesOutstanding")
    }

    private fun profitPerShare(model: Model): Item {
        return model.allItems().find { it.name == EarningsPerShareDiluted }
            ?: error("Cannot find item with name $EarningsPerShareDiluted")
    }

}