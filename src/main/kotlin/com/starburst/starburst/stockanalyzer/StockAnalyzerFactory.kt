package com.starburst.starburst.stockanalyzer

import com.mongodb.client.MongoDatabase
import com.starburst.starburst.alphavantage.AlphaVantageService
import com.starburst.starburst.edgar.explorer.EdgarExplorer
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.provider.FilingProviderFactory
import com.starburst.starburst.filingentity.FilingEntityManager
import com.starburst.starburst.models.CellGenerator
import com.starburst.starburst.stockanalyzer.analyzers.Normal
import com.starburst.starburst.stockanalyzer.analyzers.Recovery
import com.starburst.starburst.stockanalyzer.common.StockAnalyzerDataProvider
import com.starburst.starburst.stockanalyzer.dataclasses.StockAnalysis
import com.starburst.starburst.stockanalyzer.overrides.ModelOverrideService
import com.starburst.starburst.zacks.se.ZacksEstimatesService
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class StockAnalyzerFactory(
    mongoDatabase: MongoDatabase,
    private val factBase: FactBase,
    private val modelOverrideService: ModelOverrideService,
    private val filingProviderFactory: FilingProviderFactory,
    private val zacksEstimatesService: ZacksEstimatesService,
    private val edgarExplorer: EdgarExplorer,
    private val filingEntityManager: FilingEntityManager,
    private val alphaVantageService: AlphaVantageService,
) {

    companion object {
        const val Recovery = "Recovery"
        const val Normal = "Normal"
    }

    private val col = mongoDatabase.getCollection<StockAnalysis>()
    private val log = LoggerFactory.getLogger(StockAnalyzerFactory::class.java)

    fun excelModel(cik: String): ByteArray {
        val stockAnalysis = getAnalysis(cik)
        return CellGenerator.exportToXls(stockAnalysis.model, stockAnalysis.cells)
    }

    fun save(stockAnalysis: StockAnalysis) {
        col.save(stockAnalysis)
        log.info("Saved stock analysis for _id=${stockAnalysis._id}, cik=${stockAnalysis.cik}")
    }

    fun analyze(cik: String): StockAnalysis {
        val cik = cik.padStart(10, '0')
        val filingEntity = filingEntityManager.getFilingEntity(cik) ?: error("...")
        val adsh = edgarExplorer
            .latestFiscalFiling(cik)?.adsh
            ?: error("Unable to find latest fiscal filing for $cik")

        val dataProvider = StockAnalyzerDataProvider(
            filingProvider = filingProviderFactory.createFilingProvider(filingEntity.cik, adsh),
            factBase = factBase,
            filingEntity = filingEntity,
            zacksEstimatesService = zacksEstimatesService,
            modelOverrideService = modelOverrideService,
        )

        val stockAnalysis = when (filingEntity.modelTemplate?.template) {
            Recovery -> {
                Recovery(dataProvider = dataProvider).analyze()
            }
            Normal -> {
                Normal(dataProvider).analyze()
            }
            else -> error("No analysis template found for $cik")
        }

        return withCurrentPrice(stockAnalysis)
    }

    fun getAnalyses(): List<StockAnalysis> {
        return col.find().map { analysis -> withCurrentPrice(analysis) }.toList()
    }

    fun getAnalysis(cik: String): StockAnalysis {
        val cik = cik.padStart(10, '0')
        val stockAnalysis = col.findOneById(cik) ?: error("No analysis can be found for $cik")
        return withCurrentPrice(stockAnalysis)
    }

    private fun withCurrentPrice(stockAnalysis: StockAnalysis): StockAnalysis {
        val currentPrice = alphaVantageService.latestPrice(ticker = stockAnalysis.model.symbol ?: error("..."))
        return stockAnalysis.copy(currentPrice = currentPrice)
    }

}