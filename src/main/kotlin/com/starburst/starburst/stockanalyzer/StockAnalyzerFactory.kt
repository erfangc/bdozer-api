package com.starburst.starburst.stockanalyzer

import com.mongodb.client.MongoDatabase
import com.starburst.starburst.models.CellGenerator
import com.starburst.starburst.stockanalyzer.dataclasses.StockAnalysis
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Deprecated("do not use")
@Service
class StockAnalyzerFactory(
    mongoDatabase: MongoDatabase,

) {
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

    fun getAnalyses(): List<StockAnalysis> {
        return col.find().toList()
    }

    fun getAnalysis(cik: String): StockAnalysis {
        val cik = cik.padStart(10, '0')
        return col.findOneById(cik) ?: error("No analysis can be found for $cik")
    }

}