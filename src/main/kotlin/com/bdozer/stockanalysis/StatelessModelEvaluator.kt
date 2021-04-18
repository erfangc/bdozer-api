package com.bdozer.stockanalysis

import com.bdozer.models.ModelEvaluator
import com.bdozer.models.Utility
import com.bdozer.models.Utility.PresentValuePerShare
import com.bdozer.models.Utility.TerminalValuePerShare
import com.bdozer.models.dataclasses.Item
import com.bdozer.models.dataclasses.Model
import com.bdozer.stockanalysis.dataclasses.EvaluateModelRequest
import com.bdozer.stockanalysis.dataclasses.EvaluateModelResponse
import com.bdozer.stockanalysis.dataclasses.StockAnalysis2
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
    fun evaluate(request: EvaluateModelRequest): EvaluateModelResponse {
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


        val model = request.model.copy(otherItems = otherItems(request.model))
        val evaluateModelResult = ModelEvaluator().evaluate(model)
        val derivedStockAnalytics = postEvaluationAnalyzer.computeDerivedAnalytics(evaluateModelResult)

        return EvaluateModelResponse(
            cells = evaluateModelResult.cells,
            derivedStockAnalytics = derivedStockAnalytics,
        )
    }


    /**
     * Create items that would end up computing the NPV of the investment
     */
    private fun otherItems(model: Model): List<Item> {
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

