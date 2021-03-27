package com.starburst.starburst.modelbuilder

import com.mongodb.client.MongoDatabase
import com.starburst.starburst.alphavantage.AlphaVantageService
import com.starburst.starburst.edgar.explorer.EdgarExplorer
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.provider.FilingProviderFactory
import com.starburst.starburst.filingentity.FilingEntityManager
import com.starburst.starburst.filingentity.dataclasses.FilingEntity
import com.starburst.starburst.modelbuilder.common.StockAnalysis
import com.starburst.starburst.modelbuilder.templates.EarningsRecoveryAnalyzer
import com.starburst.starburst.models.CellGenerator
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
    private val filingProviderFactory: FilingProviderFactory,
    private val zacksEstimatesService: ZacksEstimatesService,
    private val edgarExplorer: EdgarExplorer,
    private val filingEntityManager: FilingEntityManager,
    private val alphaVantageService: AlphaVantageService,
) {

    private val col = mongoDatabase.getCollection<StockAnalysis>()
    private val log = LoggerFactory.getLogger(StockAnalyzerFactory::class.java)

    fun excelModel(cik: String): ByteArray {
        val stockAnalysis = getAnalysis(cik)
        return CellGenerator.exportToXls(stockAnalysis.model, stockAnalysis.cells)
    }

    fun analyze(cik: String, save: Boolean?): StockAnalysis {
        val cik = cik.padStart(10, '0')
        val filingEntity = filingEntityManager.getFilingEntity(cik)
        val edgarFilingMetadata =
            edgarExplorer.latestFiscalFiling(cik) ?: error("Unable to find latest fiscal filing for $cik")
        val adsh = edgarFilingMetadata.adsh
        val stockAnalysis = when (filingEntity?.modelTemplate?.template) {
            "Recovery" -> {
                earningRecoveryAnalyzer(filingEntity, adsh).analyze()
            }
            else -> error("No analysis template found for $cik")
        }
        if (save == true) {
            col.save(stockAnalysis)
            log.info("Saved stock analysis for _id=${stockAnalysis._id}, cik=$cik")
        }
        return stockAnalysis
    }

    fun getAnalyses(): List<StockAnalysis> {
        return col.find().map { analysis -> updateCurrentPrice(analysis) }.toList()
    }

    private fun earningRecoveryAnalyzer(filingEntity: FilingEntity, adsh: String) = EarningsRecoveryAnalyzer(
        filingProvider = filingProviderFactory.createFilingProvider(filingEntity.cik, adsh),
        factBase = factBase,
        filingEntity = filingEntity,
        zacksEstimatesService = zacksEstimatesService,
    )

    fun getAnalysis(cik: String): StockAnalysis {
        val cik = cik.padStart(10, '0')
        val stockAnalysis = col.findOneById(cik) ?: error("No analysis can be found for $cik")
        return updateCurrentPrice(stockAnalysis)
    }

    private fun updateCurrentPrice(stockAnalysis: StockAnalysis): StockAnalysis {
        val currentPrice = alphaVantageService.latestPrice(ticker = stockAnalysis.model.symbol ?: error("..."))
        return stockAnalysis.copy(currentPrice = currentPrice)
    }
}