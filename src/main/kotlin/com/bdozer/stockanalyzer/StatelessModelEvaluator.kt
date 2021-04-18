package com.bdozer.stockanalyzer

import com.bdozer.models.ModelEvaluator
import com.bdozer.models.Utility
import com.bdozer.models.Utility.PresentValuePerShare
import com.bdozer.models.Utility.TerminalValuePerShare
import com.bdozer.models.dataclasses.Item
import com.bdozer.models.dataclasses.Model
import com.bdozer.stockanalyzer.analyzers.extensions.PostEvaluationAnalyzer
import com.bdozer.stockanalyzer.dataclasses.EvaluateModelRequest
import com.bdozer.stockanalyzer.dataclasses.StockAnalysis2
import org.springframework.stereotype.Service

@Service
class StatelessModelEvaluator(private val postEvaluationAnalyzer: PostEvaluationAnalyzer) {

    companion object {
        fun Model.allItems(): List<Item> {
            return (incomeStatementItems + balanceSheetItems + cashFlowStatementItems + otherItems)
        }
    }


    /**
     * Evaluate the given model and return a [StockAnalysis2]
     */
    fun evaluate(request: EvaluateModelRequest): StockAnalysis2 {
        /*
        perform validation
         */
        request.model.epsConceptName ?: error("Please define ${Model::epsConceptName.name} on the model")
        request.model.allItems().find { it.name == request.model.epsConceptName }
            ?: error("There must be an Item named ${request.model.epsConceptName} in your model")

        request.model.netIncomeConceptName ?: error("Please define ${Model::netIncomeConceptName.name} on the model")
        request.model.allItems().find { it.name == request.model.netIncomeConceptName }
            ?: error("There must be an Item named ${request.model.netIncomeConceptName} in your model")

        request.model.totalRevenueConceptName ?: error("Please define ${Model::totalRevenueConceptName.name} on the model")
        request.model.allItems().find { it.name == request.model.totalRevenueConceptName }
            ?: error("There must be an Item named ${request.model.totalRevenueConceptName} in your model")

        request.model.sharesOutstandingConceptName
            ?: error("Please define ${Model::sharesOutstandingConceptName.name} on the model")
        request.model.allItems().find { it.name == request.model.sharesOutstandingConceptName }
            ?: error("There must be an Item named ${request.model.sharesOutstandingConceptName} in your model")


        val model = request.model.copy(otherItems = dcfItems(request.model))
        val evaluateModelResult = ModelEvaluator().evaluate(model)
        val derivedStockAnalytics = postEvaluationAnalyzer.computeDerivedAnalytics(evaluateModelResult)

        return StockAnalysis2(
            name = request.name,
            model = model,
            description = request.description,
            cells = evaluateModelResult.cells,
            cik = model.cik,
            ticker = model.ticker,
            derivedStockAnalytics = derivedStockAnalytics,
        )

    }


    /**
     * Create items that would end up computing the NPV of the investment
     */
    private fun dcfItems(model: Model): List<Item> {
        val epsConceptName = model.epsConceptName
        val periods = model.periods
        val discountRate = (model.equityRiskPremium * model.beta) + model.riskFreeRate
        val terminalPeMultiple = 1.0 / (discountRate - model.terminalGrowthRate)

        return listOf(
            Item(
                name = Utility.DiscountFactor,
                formula = "1 / (1.0 + $discountRate)^period",
            ),
            Item(
                name = TerminalValuePerShare,
                formula = "if(period=$periods,$epsConceptName * $terminalPeMultiple,0.0)",
            ),
            Item(
                name = Utility.PresentValueOfTerminalValuePerShare,
                formula = "${Utility.DiscountFactor} * $TerminalValuePerShare",
            ),
            Item(
                name = Utility.PresentValueOfEarningsPerShare,
                formula = "${Utility.DiscountFactor} * $epsConceptName",
            ),
            Item(
                name = PresentValuePerShare,
                formula = "${Utility.PresentValueOfEarningsPerShare} + ${Utility.PresentValueOfTerminalValuePerShare}",
            )
        )
    }

}

