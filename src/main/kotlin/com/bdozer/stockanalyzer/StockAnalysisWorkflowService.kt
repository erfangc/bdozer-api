package com.bdozer.stockanalyzer

import com.bdozer.alphavantage.AlphaVantageService
import com.bdozer.edgar.explorer.EdgarExplorer
import com.bdozer.edgar.factbase.FactBase
import com.bdozer.edgar.provider.FilingProviderFactory
import com.bdozer.filingentity.FilingEntityManager
import com.bdozer.filingentity.dataclasses.FilingEntity
import com.bdozer.models.CellGenerator
import com.bdozer.stockanalyzer.analyzers.Normal
import com.bdozer.stockanalyzer.analyzers.Recovery
import com.bdozer.stockanalyzer.analyzers.StockAnalyzerDataProvider
import com.bdozer.stockanalyzer.dataclasses.StockAnalysis2
import com.bdozer.zacks.se.ZacksEstimatesService
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service

@Service
class StockAnalysisWorkflowService(
    private val stockAnalysisCRUDService: StockAnalysisCRUDService,
    private val factBase: FactBase,
    private val filingProviderFactory: FilingProviderFactory,
    private val alphaVantageService: AlphaVantageService,
    private val zacksEstimatesService: ZacksEstimatesService,
    private val edgarExplorer: EdgarExplorer,
    private val filingEntityManager: FilingEntityManager,
) {

    fun download(id: String): HttpEntity<ByteArray> {
        val stockAnalysis = stockAnalysisCRUDService.get(id) ?: error("...")
        val headers = HttpHeaders()
        headers.set(HttpHeaders.CONTENT_TYPE, "application/vnd.ms-excel")
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=${stockAnalysis.cik}.xlsx")
        val byteArray = CellGenerator.exportToXls(stockAnalysis.model, stockAnalysis.cells)
        return HttpEntity(byteArray, headers)
    }

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
        alphaVantageService = alphaVantageService,
    )

}