package com.bdozer.api.stockanalysis.support

import com.bdozer.api.models.CellEvaluator
import com.bdozer.api.models.CellGenerator
import com.bdozer.api.models.dataclasses.Model
import com.bdozer.api.stockanalysis.dataclasses.EvaluateModelRequest
import com.bdozer.api.stockanalysis.dataclasses.StockAnalysis2
import com.bdozer.api.stockanalysis.support.poylgon.PolygonService
import org.apache.http.client.HttpClient

/**
 * [ModelEvaluator] evaluates the provided [Model] and runs [DerivedAnalyticsComputer]
 * on the output forming a [StockAnalysis2]
 *
 * It is stateless in the sense that this service does not CRUD or manage the lifecycle of the analysis
 * which makes it ideal for on-the-fly reruns
 */
class ModelEvaluator(httpClient: HttpClient) {

    private val polygonService = PolygonService(httpClient)
    private val derivedAnalyticsComputer = DerivedAnalyticsComputer(polygonService)

    /**
     * Evaluate the given model and return a [StockAnalysis2]
     */
    fun evaluate(request: EvaluateModelRequest): StockAnalysis2 {
        /*
        perform validation
         */
        validateRequestOrThrow(request)
        val model = request
            .model
            .copy(otherItems = request.model.generateOtherItems())

        /*
        overlay items form the override to model
         */
        val overriddenModel = model.withOverrides()
        val cells = CellEvaluator()
            .evaluate(
                CellGenerator()
                    .generateCells(overriddenModel)
            )

        val derivedStockAnalytics = derivedAnalyticsComputer.computeDerivedAnalytics(
            model = overriddenModel,
            cells = cells,
        )


        val stockAnalysis2 = StockAnalysis2(
            name = model.name,
            cik = model.cik,
            ticker = model.ticker,
            model = model,
            cells = cells,
            derivedStockAnalytics = derivedStockAnalytics,
        )

        /*
        Grab a bunch of things from Polygon.io and throw them onto the analysis
         */
        return model.ticker?.let { ticker ->
            val tickerDetail = polygonService.tickerDetails(ticker = ticker)
            stockAnalysis2.copy(
                description = tickerDetail.description,
                industry = tickerDetail.industry,
                sector = tickerDetail.sector,
                url = tickerDetail.url,
                similar = tickerDetail.similar,
                ceo = tickerDetail.ceo,
                country = tickerDetail.country,
            )
        } ?: stockAnalysis2
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

