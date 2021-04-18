package com.bdozer.stockanalyzer

import com.bdozer.stockanalyzer.dataclasses.EvaluateModelRequest
import com.bdozer.stockanalyzer.dataclasses.EvaluateModelResponse
import com.bdozer.stockanalyzer.dataclasses.FindStockAnalysisResponse
import com.bdozer.stockanalyzer.dataclasses.StockAnalysis2
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.TextSearchOptions
import io.swagger.v3.oas.annotations.Operation
import org.litote.kmongo.*
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.time.Instant

@Service
class StockAnalysisService(
    mongoDatabase: MongoDatabase,
    private val statelessModelEvaluator: StatelessModelEvaluator,
) {

    val col = mongoDatabase.getCollection<StockAnalysis2>()

    fun refreshStockAnalysis(stockAnalysis: StockAnalysis2): StockAnalysis2 {
        val request = EvaluateModelRequest(model = stockAnalysis.model)
        val evaluateModelResponse = statelessModelEvaluator.evaluate(request = request)
        return stockAnalysis.copy(
            derivedStockAnalytics = evaluateModelResponse.derivedStockAnalytics,
            cells = evaluateModelResponse.cells,
        )
    }

    fun evaluateStockAnalysis(request: EvaluateModelRequest): EvaluateModelResponse {
        return statelessModelEvaluator.evaluate(request)
    }

    fun saveStockAnalysis(analysis: StockAnalysis2) {
        col.save(analysis.copy(lastUpdated = Instant.now()))
    }

    fun deleteStockAnalysis(id: String) {
        col.deleteOneById(id)
    }

    fun getStockAnalysis(id: String): StockAnalysis2? {
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
        val stockAnalysis = (getStockAnalysis(id) ?: error("..."))
            .copy(published = true, lastUpdated = Instant.now())
        saveStockAnalysis(stockAnalysis)
        return stockAnalysis
    }

    fun unpublish(id: String): StockAnalysis2 {
        val stockAnalysis = (getStockAnalysis(id) ?: error("..."))
            .copy(published = false, lastUpdated = Instant.now())
        saveStockAnalysis(stockAnalysis)
        return stockAnalysis
    }

}
