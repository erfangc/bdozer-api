package com.bdozer.stockanalyzer

import com.bdozer.stockanalyzer.dataclasses.FindStockAnalysisResponse
import com.bdozer.stockanalyzer.dataclasses.StockAnalysis2
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.TextSearchOptions
import org.litote.kmongo.*
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class StockAnalysisService(mongoDatabase: MongoDatabase) {

    val col = mongoDatabase.getCollection<StockAnalysis2>()

    fun save(analysis: StockAnalysis2) {
        col.save(analysis.copy(lastUpdated = Instant.now()))
    }

    fun delete(id: String) {
        col.deleteOneById(id)
    }

    fun get(id: String): StockAnalysis2? {
        return col.findOneById(id)
    }

    fun find(
        userId: String? = null,
        cik: String? = null,
        ticker: String? = null,
        published: Boolean? = null,
        skip: Int? = null,
        limit: Int? = null,
        term: String? = null,
    ): FindStockAnalysisResponse {

        val filter = and(
            userId?.let { StockAnalysis2::userId eq it },
            published?.let { StockAnalysis2::published eq it },
            cik?.let { StockAnalysis2::cik eq it.padStart(10, '0') },
            ticker?.let { StockAnalysis2::ticker eq it },
            term?.let { text(it, TextSearchOptions().caseSensitive(false)) },
        )

        val found = col.find(filter)
        val totalCount = found.count()

        val stockAnalyses = found
            .skip(skip ?: 0)
            .limit(limit ?: 10)
            .sort(descending(StockAnalysis2::lastUpdated))
            .toList()

        return FindStockAnalysisResponse(
            totalCount = totalCount,
            stockAnalyses = stockAnalyses,
        )

    }

    fun publish(id: String): StockAnalysis2 {
        val stockAnalysis = (get(id) ?: error("..."))
            .copy(published = true, lastUpdated = Instant.now())
        save(stockAnalysis)
        return stockAnalysis
    }

    fun unpublish(id: String): StockAnalysis2 {
        val stockAnalysis = (get(id) ?: error("..."))
            .copy(published = false, lastUpdated = Instant.now())
        save(stockAnalysis)
        return stockAnalysis
    }

}
