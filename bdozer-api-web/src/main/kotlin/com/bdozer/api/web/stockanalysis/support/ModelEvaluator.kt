package com.bdozer.api.web.stockanalysis.support

import com.bdozer.api.models.CellEvaluator
import com.bdozer.api.models.CellGenerator
import com.bdozer.api.models.dataclasses.Model
import com.bdozer.api.stockanalysis.models.StockAnalysis2
import com.bdozer.api.web.stockanalysis.support.poylgon.PolygonService
import com.bdozer.api.web.stockanalysis.support.zacks.ZacksDerivedAnalyticsCalculator
import org.springframework.stereotype.Service

/**
 * [ModelEvaluator] evaluates the provided [Model] and runs [DerivedAnalyticsComputer]
 * on the output forming a [StockAnalysis2]
 *
 * It is stateless in the sense that this service does not CRUD or manage the lifecycle of the analysis
 * which makes it ideal for on-the-fly reruns
 */
@Service
class ModelEvaluator(
    private val polygonService: PolygonService,
    private val derivedAnalyticsComputer: DerivedAnalyticsComputer,
    private val zacksDerivedAnalyticsCalculator: ZacksDerivedAnalyticsCalculator,
) {

    fun evaluate(model: Model) = evaluate(StockAnalysis2(model = model))

    /**
     * Evaluate the given model and return a [StockAnalysis2]
     */
    fun evaluate(existing: StockAnalysis2): StockAnalysis2 {
        val model = existing.model.let {
            it.copy(
                otherItems = it.otherItems.ifEmpty {
                    it.generateOtherItems()
                }
            )
        }

        /*
        perform validation
         */
        validateRequestOrThrow(model)

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


        val stockAnalysis2 = existing.copy(
            name = model.name,
            cik = model.cik,
            model = model,
            cells = cells,
            derivedStockAnalytics = derivedStockAnalytics,
        )
        
        /*
        Grab a bunch of things from Polygon.io and throw them onto the analysis
         */
        return model.ticker?.let { ticker ->
            val tickerDetail = polygonService.tickerDetails(ticker = ticker).results
            val zacksDerivedAnalytics = zacksDerivedAnalyticsCalculator.getZacksDerivedAnalytics(ticker = ticker)
            stockAnalysis2.copy(
                ticker = model.ticker,
                description = tickerDetail?.description,
                industry = tickerDetail?.sicDescription,
                url = tickerDetail?.homepageUrl,
                tags = stockAnalysis2.tags,
                derivedStockAnalytics = derivedStockAnalytics.copy(marketCap = tickerDetail?.marketCap),
                zacksDerivedAnalytics = zacksDerivedAnalytics,
            )
        } ?: stockAnalysis2
    }

    private fun validateRequestOrThrow(model: Model) {
        model.epsConceptName ?: error("Please define ${Model::epsConceptName.name} on the model")
        model.allItems().find { it.name == model.epsConceptName }
            ?: error("There must be an Item named ${model.epsConceptName} in your model")

        model.netIncomeConceptName ?: error("Please define ${Model::netIncomeConceptName.name} on the model")
        model.allItems().find { it.name == model.netIncomeConceptName }
            ?: error("There must be an Item named ${model.netIncomeConceptName} in your model")

        model.totalRevenueConceptName
            ?: error("Please define ${Model::totalRevenueConceptName.name} on the model")
        model.allItems().find { it.name == model.totalRevenueConceptName }
            ?: error("There must be an Item named ${model.totalRevenueConceptName} in your model")

        model.sharesOutstandingConceptName
            ?: error("Please define ${Model::sharesOutstandingConceptName.name} on the model")
        model.allItems().find { it.name == model.sharesOutstandingConceptName }
            ?: error("There must be an Item named ${model.sharesOutstandingConceptName} in your model")
    }

}

