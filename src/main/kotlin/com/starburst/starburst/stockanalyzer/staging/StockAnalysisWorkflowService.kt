package com.starburst.starburst.stockanalyzer.staging

import com.starburst.starburst.edgar.explorer.EdgarExplorer
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.provider.FilingProviderFactory
import com.starburst.starburst.filingentity.FilingEntityManager
import com.starburst.starburst.filingentity.dataclasses.FilingEntity
import com.starburst.starburst.stockanalyzer.analyzers.Normal
import com.starburst.starburst.stockanalyzer.analyzers.Recovery
import com.starburst.starburst.stockanalyzer.common.StockAnalyzerDataProvider
import com.starburst.starburst.stockanalyzer.staging.dataclasses.StockAnalysis2
import com.starburst.starburst.zacks.se.ZacksEstimatesService
import org.springframework.stereotype.Service

@Service
class StockAnalysisWorkflowService(
    private val factBase: FactBase,
    private val filingProviderFactory: FilingProviderFactory,
    private val zacksEstimatesService: ZacksEstimatesService,
    private val edgarExplorer: EdgarExplorer,
    private val filingEntityManager: FilingEntityManager,
) {

    fun create(cik: String): StockAnalysis2 {
        return run(StockAnalysis2(cik = cik))
    }

    fun refresh(stockAnalysis: StockAnalysis2): StockAnalysis2 {
        return run(stockAnalysis)
    }

    private fun run(stockAnalysis: StockAnalysis2): StockAnalysis2 {
        val cik = stockAnalysis.cik ?: error("stockAnalysis.cik not populated")
        val filingEntity = filingEntityManager.getFilingEntity(cik) ?: error("filingEntity not found for $cik")

        val adsh = latestAdsh(filingEntity)
        val dataProvider = createDataProvider(filingEntity, adsh)

        return when (filingEntity.modelTemplate?.template) {
            "Normal" -> {
                Normal(dataProvider, stockAnalysis).analyze()
            }
            "Recovery" -> {
                Recovery(dataProvider, stockAnalysis).analyze()
            }
            else -> error("model template not specified for $cik")
        }
    }

    private fun latestAdsh(filingEntity: FilingEntity) = (edgarExplorer
        .latestFiscalFiling(filingEntity.cik)?.adsh
        ?: error("Unable to find latest fiscal filing for ${filingEntity.cik}"))

    private fun createDataProvider(filingEntity: FilingEntity, adsh: String) = StockAnalyzerDataProvider(
        filingProvider = filingProviderFactory.createFilingProvider(filingEntity.cik, adsh),
        factBase = factBase,
        filingEntity = filingEntity,
        zacksEstimatesService = zacksEstimatesService,
    )

}