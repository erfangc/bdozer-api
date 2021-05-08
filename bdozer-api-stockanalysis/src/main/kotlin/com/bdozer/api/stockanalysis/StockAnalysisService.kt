package com.bdozer.api.stockanalysis

import com.bdozer.api.stockanalysis.dataclasses.*
import com.bdozer.api.stockanalysis.support.StatelessModelEvaluator
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoDatabase
import org.bson.*
import org.litote.kmongo.*
import java.time.Instant

class StockAnalysisService(
    mongoDatabase: MongoDatabase,
    private val statelessModelEvaluator: StatelessModelEvaluator,
) {

    private val stockAnalyses = mongoDatabase.getCollection<StockAnalysis2>()
    private val collectionStockAnalyses = mongoDatabase.getCollection("stockAnalysis2")

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
        stockAnalyses.save(analysis.copy(lastUpdated = Instant.now()))
    }

    fun deleteStockAnalysis(id: String) {
        stockAnalyses.deleteOneById(id)
    }

    fun getStockAnalysis(id: String): StockAnalysis2? {
        return stockAnalyses.findOneById(id)
    }

    fun allAnalyses(): FindIterable<StockAnalysis2> {
        return stockAnalyses.find()
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

        val termCondition = term?.let { term ->
            BsonDocument(
                "text",
                BsonDocument()
                    .append("query", BsonString(term))
                    .append(
                        "path",
                        BsonArray(
                            listOf(
                                BsonString(StockAnalysis2::name.name),
                                BsonString(StockAnalysis2::ticker.name)
                            )
                        )
                    )
            )
        }

        val tagsConditions = tags?.map { tag ->
            BsonDocument(
                "text",
                BsonDocument()
                    .append("query", BsonString(tag))
                    .append("path", BsonString(StockAnalysis2::tags.name))
            )
        } ?: emptyList()

        val userIdCondition = userId?.let { it ->
            BsonDocument(
                "text",
                BsonDocument()
                    .append("query", BsonString(it))
                    .append("path", BsonString(StockAnalysis2::userId.name))
            )
        }

        val tickerCondition = ticker?.let { it ->
            BsonDocument(
                "text",
                BsonDocument()
                    .append("query", BsonString(it))
                    .append("path", BsonString(StockAnalysis2::ticker.name))
            )
        }

        val cikCondition = cik?.let { it ->
            BsonDocument(
                "text",
                BsonDocument()
                    .append("query", BsonString(it))
                    .append("path", BsonString(StockAnalysis2::cik.name))
            )
        }

        val publishedCondition = published?.let { it ->
            BsonDocument(
                "equals",
                BsonDocument()
                    .append("value", BsonBoolean(it))
                    .append("path", BsonString(StockAnalysis2::published.name))
            )
        }

        val musts = listOfNotNull(
            termCondition,
            tickerCondition,
            publishedCondition,
            cikCondition,
            userIdCondition,
        ) + tagsConditions


        val search = if (musts.isNotEmpty()) BsonDocument()
            .append(
                "\$search",
                BsonDocument(
                    "compound",
                    BsonDocument(
                        "must",
                        BsonArray(musts)
                    )
                )
            )
        else
            null
        val iterable = collectionStockAnalyses
            .aggregate(
                listOfNotNull(
                    search,
                    BsonDocument(
                        "\$project",
                        BsonDocument()
                            .append(StockAnalysis2::_id.name, BsonInt32(1))
                            .append(StockAnalysis2::ticker.name, BsonInt32(1))
                            .append(StockAnalysis2::cik.name, BsonInt32(1))
                            .append(StockAnalysis2::name.name, BsonInt32(1))
                            .append(StockAnalysis2::published.name, BsonInt32(1))
                            .append(StockAnalysis2::lastUpdated.name, BsonInt32(1))
                            .append(
                                (StockAnalysis2::derivedStockAnalytics / DerivedStockAnalytics::targetPrice).name,
                                BsonInt32(1)
                            )
                            .append(
                                (StockAnalysis2::derivedStockAnalytics / DerivedStockAnalytics::currentPrice).name,
                                BsonInt32(1)
                            )
                    ),
                    BsonDocument("\$skip", BsonInt32(skip ?: 0)),
                    BsonDocument("\$limit", BsonInt32((limit ?: 10))),
                )
            )

        val stockAnalyses = iterable
            .map { doc ->
                val derivedAnalytics = doc.get(StockAnalysis2::derivedStockAnalytics.name, Document::class.java)
                val targetPrice = derivedAnalytics?.getDouble(DerivedStockAnalytics::targetPrice.name)
                val currentPrice = derivedAnalytics?.getDouble(DerivedStockAnalytics::currentPrice.name)
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
