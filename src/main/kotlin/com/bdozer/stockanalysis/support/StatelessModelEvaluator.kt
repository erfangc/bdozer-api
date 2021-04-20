package com.bdozer.stockanalysis.support

import com.bdozer.models.ModelEvaluator
import com.bdozer.models.dataclasses.Item
import com.bdozer.models.dataclasses.Model
import com.bdozer.stockanalysis.dataclasses.EvaluateModelRequest
import com.bdozer.stockanalysis.dataclasses.EvaluateModelResponse
import com.bdozer.stockanalysis.dataclasses.StockAnalysis2
import org.springframework.stereotype.Service

@Service
class StatelessModelEvaluator(private val derivedAnalyticsComputer: DerivedAnalyticsComputer) {

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
        validateRequestOrThrow(request)
        val model = request
            .model
            .copy(otherItems = request.model.generateOtherItems())
        val evaluateModelResult = ModelEvaluator().evaluate(model)
        val derivedStockAnalytics = derivedAnalyticsComputer.computeDerivedAnalytics(evaluateModelResult)

        return EvaluateModelResponse(
            model = model,
            cells = evaluateModelResult.cells,
            derivedStockAnalytics = derivedStockAnalytics,
        )
    }

    private fun validateRequestOrThrow(request: EvaluateModelRequest) {
        request.model.epsConceptName ?: error("Please define ${Model::epsConceptName.name} on the model")
        request.model.allItems().find { it.name == request.model.epsConceptName }
            ?: error("There must be an Item named ${request.model.epsConceptName} in your model")

        request.model.netIncomeConceptName ?: error("Please define ${Model::netIncomeConceptName.name} on the model")
        request.model.allItems().find { it.name == request.model.netIncomeConceptName }
            ?: error("There must be an Item named ${request.model.netIncomeConceptName} in your model")

        request.model.totalRevenueConceptName
            ?: error("Please define ${Model::totalRevenueConceptName.name} on the model")
        request.model.allItems().find { it.name == request.model.totalRevenueConceptName }
            ?: error("There must be an Item named ${request.model.totalRevenueConceptName} in your model")

        request.model.sharesOutstandingConceptName
            ?: error("Please define ${Model::sharesOutstandingConceptName.name} on the model")
        request.model.allItems().find { it.name == request.model.sharesOutstandingConceptName }
            ?: error("There must be an Item named ${request.model.sharesOutstandingConceptName} in your model")
    }

}

