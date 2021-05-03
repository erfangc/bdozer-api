package com.bdozer.api.web.stockanalysis.support

import com.bdozer.api.web.models.ModelEvaluator
import bdozer.api.common.model.Model
import bdozer.api.common.stockanalysis.EvaluateModelRequest
import bdozer.api.common.stockanalysis.EvaluateModelResponse
import bdozer.api.common.stockanalysis.StockAnalysis2
import org.springframework.stereotype.Service

/**
 * [StatelessModelEvaluator] evaluates the provided [Model] and runs [DerivedAnalyticsComputer]
 * on the output forming a [StockAnalysis2]
 *
 * It is stateless in the sense that this service does not CRUD or manage the lifecycle of the analysis
 * which makes it ideal for on-the-fly reruns
 */
@Service
class StatelessModelEvaluator(private val derivedAnalyticsComputer: DerivedAnalyticsComputer) {

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

