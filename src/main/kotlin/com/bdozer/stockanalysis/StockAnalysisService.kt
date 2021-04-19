package com.bdozer.stockanalysis

import com.bdozer.stockanalysis.dataclasses.*
import com.bdozer.stockanalysis.support.StatelessModelEvaluator
import com.bdozer.stockanalysis.dataclasses.StockAnalysisProjection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Projections
import com.mongodb.client.model.TextSearchOptions
import org.bson.Document
import org.litote.kmongo.*
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class StockAnalysisService(
    mongoDatabase: MongoDatabase,
    private val statelessModelEvaluator: StatelessModelEvaluator,
) {

    val col = mongoDatabase.getCollection<StockAnalysis2>()
    val collection = mongoDatabase.getCollection("stockAnalysis2")

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

    fun findStockAnalyses(
        userId: String? = null,
        cik: String? = null,
        ticker: String? = null,
        published: Boolean? = null,
        skip: Int? = null,
        limit: Int? = null,
        term: String? = null,
        tags: List<String>? = null,
    ): FindStockAnalysisResponse {

        val filter = and(
            userId?.let { StockAnalysis2::userId eq it },
            published?.let { StockAnalysis2::published eq it },
            tags?.let { tgs -> or(tgs.map { StockAnalysis2::tags contains it  }) },
            cik?.let { StockAnalysis2::cik eq it.padStart(10, '0') },
            ticker?.let { StockAnalysis2::ticker eq it },
            term?.let { text(it, TextSearchOptions().caseSensitive(false)) },
        )
        val iterable = collection
            .find(filter)
            .projection(
                Projections.fields(
                    Projections.include(
                        StockAnalysis2::_id.name,
                        StockAnalysis2::name.name,
                        StockAnalysis2::description.name,
                        StockAnalysis2::cik.name,
                        StockAnalysis2::ticker.name,
                        (StockAnalysis2::derivedStockAnalytics / DerivedStockAnalytics::currentPrice).name,
                        (StockAnalysis2::derivedStockAnalytics / DerivedStockAnalytics::targetPrice).name,
                        StockAnalysis2::published.name,
                        StockAnalysis2::lastUpdated.name,
                    )
                )
            )

        val totalCount = iterable.count()

        val stockAnalyses = iterable
            .skip(skip ?: 0)
            .limit(limit ?: 10)
            .sort(descending(StockAnalysis2::lastUpdated))
            .map {
                doc ->
                val derivedAnalytics = doc.get(StockAnalysis2::derivedStockAnalytics.name, Document::class.java)
                val targetPrice = derivedAnalytics.getDouble(DerivedStockAnalytics::targetPrice.name)
                val currentPrice = derivedAnalytics.getDouble(DerivedStockAnalytics::currentPrice.name)
                StockAnalysisProjection(
                    _id = doc.getString(StockAnalysis2::_id.name),
                    name = doc.getString(StockAnalysis2::name.name),
                    description = doc.getString(StockAnalysis2::description.name),
                    cik = doc.getString(StockAnalysis2::cik.name),
                    ticker = doc.getString(StockAnalysis2::ticker.name),
                    currentPrice = currentPrice,
                    targetPrice = targetPrice,
                    published = doc.getBoolean(StockAnalysis2::published.name),
                    lastUpdated = doc.getDate(StockAnalysis2::lastUpdated.name).toInstant(),
                )
            }
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
