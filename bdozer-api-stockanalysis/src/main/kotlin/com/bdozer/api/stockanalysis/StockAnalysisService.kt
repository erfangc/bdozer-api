package com.bdozer.api.stockanalysis

import com.bdozer.api.models.dataclasses.Model
import com.bdozer.api.stockanalysis.dataclasses.FindStockAnalysisResponse
import com.bdozer.api.stockanalysis.dataclasses.StockAnalysis2
import com.bdozer.api.stockanalysis.support.ModelEvaluator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import java.time.Instant

class StockAnalysisService(
    private val restHighLevelClient: RestHighLevelClient,
    private val modelEvaluator: ModelEvaluator,
    private val objectMapper: ObjectMapper,
) {
    private val indexName = "stock-analyses"
    fun refreshStockAnalysis(stockAnalysisId: String, save: Boolean? = null): StockAnalysis2 {
        val stockAnalysis = getStockAnalysis(stockAnalysisId)
        val refreshedAnalysis = modelEvaluator.evaluate(stockAnalysis)
        if (save == true) {
            saveStockAnalysis(refreshedAnalysis)
        }
        return refreshedAnalysis
    }

    fun evaluateStockAnalysis(
        model: Model,
        saveAs: String? = null,
        published: Boolean = false,
        tags: List<String> = emptyList(),
    ): StockAnalysis2 {
        val stockAnalysis = modelEvaluator.evaluate(model = model)
        return if (saveAs != null) {
            val saveAsAnalysis = stockAnalysis.copy(_id = saveAs, published = published, tags = tags)
            saveStockAnalysis(saveAsAnalysis)
            saveAsAnalysis
        } else {
            stockAnalysis
        }
    }

    fun saveStockAnalysis(stockAnalysis: StockAnalysis2) {
        restHighLevelClient.index(
            IndexRequest(indexName)
                .id(stockAnalysis._id)
                .source(
                    objectMapper.writeValueAsString(stockAnalysis.copy(lastUpdated = Instant.now())),
                    XContentType.JSON
                ),
            RequestOptions.DEFAULT
        )
    }

    fun deleteStockAnalysis(id: String) {
        val deleteResponse = restHighLevelClient.delete(
            DeleteRequest(indexName).id(id),
            RequestOptions.DEFAULT
        )
    }

    fun getStockAnalysis(id: String): StockAnalysis2 {
        val response = restHighLevelClient.get(GetRequest(indexName).id(id), RequestOptions.DEFAULT)
        return objectMapper.readValue(response.sourceAsBytes)
    }

    fun allAnalyses(): List<StockAnalysis2> {
        val searchResponse = restHighLevelClient.search(
            SearchRequest(indexName).source(
                SearchSourceBuilder.searchSource().query(
                    QueryBuilders.matchAllQuery()
                )
            ),
            RequestOptions.DEFAULT
        )
        return searchResponse.hits.map { searchHit ->
            val json = searchHit.sourceAsString
            objectMapper.readValue(json)
        }
    }

    /**
     * This method returns the 4 most prominent and featured
     * stock analyses on the system
     */
    fun top4StockAnalyses(): FindStockAnalysisResponse {
        // TODO replace with an actual search or sort algorithm
        return findStockAnalyses(limit = 4, published = true)
    }

    /**
     * Search is accomplished via the MongoDB text search index
     * KMongo is not great for this, so we use the raw MongoDB client by
     * constructing and passing to the server BsonDocument objects
     */
    fun findStockAnalyses(
        userId: String? = null,
        cik: String? = null,
        ticker: String? = null,
        published: Boolean? = null,
        skip: Int? = null,
        limit: Int? = null,
        term: String? = null,
        tags: List<String>? = null,
        sort: SortDirection? = null,
    ): FindStockAnalysisResponse {
        // TODO
        return FindStockAnalysisResponse(stockAnalyses = emptyList())

    }

    fun publish(id: String): StockAnalysis2 {
        val stockAnalysis = getStockAnalysis(id)
            .copy(published = true, lastUpdated = Instant.now())
        saveStockAnalysis(stockAnalysis)
        return stockAnalysis
    }

    fun unpublish(id: String): StockAnalysis2 {
        val stockAnalysis = getStockAnalysis(id)
            .copy(published = false, lastUpdated = Instant.now())
        saveStockAnalysis(stockAnalysis)
        return stockAnalysis
    }

}
