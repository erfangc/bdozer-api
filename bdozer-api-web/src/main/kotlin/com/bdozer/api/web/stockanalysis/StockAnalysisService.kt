package com.bdozer.api.web.stockanalysis

import com.bdozer.api.models.dataclasses.Model
import com.bdozer.api.stockanalysis.models.FindStockAnalysisResponse
import com.bdozer.api.stockanalysis.models.StockAnalysis2
import com.bdozer.api.stockanalysis.models.StockAnalysisProjection
import com.bdozer.api.web.stockanalysis.support.ModelEvaluator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.time.Instant

@Service
class StockAnalysisService(
    private val restHighLevelClient: RestHighLevelClient,
    private val objectMapper: ObjectMapper,
    private val s3: S3Client,
    private val modelEvaluator: ModelEvaluator,
) {
    private val log = LoggerFactory.getLogger(StockAnalysisService::class.java)
    private val bucket = "saved-objects-422873008393"
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
        val updatedStockAnalysis = stockAnalysis.copy(lastUpdated = Instant.now())
        log.info("Saving analysis id=${stockAnalysis._id}")
        s3.putObject(
            PutObjectRequest
                .builder()
                .bucket(bucket)
                .key("$indexName/${stockAnalysis._id}.json")
                .build(),
            RequestBody.fromBytes(objectMapper.writeValueAsBytes(updatedStockAnalysis))
        )
        log.info("Indexing analysis id=${stockAnalysis._id}")
        // remove key fields for Elasticsearch indexing so the object is not too large
        indexProjection(updatedStockAnalysis)
    }

    private fun indexProjection(updatedStockAnalysis: StockAnalysis2) {
        val stockAnalysisProjection = StockAnalysisProjection(
            userId = updatedStockAnalysis.userId,
            name = updatedStockAnalysis.name,
            description = updatedStockAnalysis.description,
            cik = updatedStockAnalysis.cik,
            ticker = updatedStockAnalysis.ticker,
            currentPrice = updatedStockAnalysis.derivedStockAnalytics?.currentPrice,
            targetPrice = updatedStockAnalysis.derivedStockAnalytics?.targetPrice,
            finalPrice = updatedStockAnalysis.derivedStockAnalytics?.finalPrice,
            published = updatedStockAnalysis.published,
            lastUpdated = updatedStockAnalysis.lastUpdated,
            tags = updatedStockAnalysis.tags,
            zacksDerivedAnalytics = updatedStockAnalysis.zacksDerivedAnalytics,
        )
        val json = objectMapper.writeValueAsString(stockAnalysisProjection)
        restHighLevelClient.index(
            IndexRequest(indexName)
                .id(updatedStockAnalysis._id)
                .source(json, XContentType.JSON),
            RequestOptions.DEFAULT,
        )
    }

    fun deleteStockAnalysis(id: String) {
        s3.deleteObject(
            DeleteObjectRequest
                .builder()
                .bucket(bucket)
                .key("$indexName/$id.json")
                .build()
        )
        restHighLevelClient.delete(
            DeleteRequest(indexName).id(id), RequestOptions.DEFAULT
        )
    }

    fun getStockAnalysis(id: String): StockAnalysis2 {
        return objectMapper.readValue(
            s3.getObject(GetObjectRequest.builder().bucket(bucket).key("$indexName/$id.json").build())
        )
    }

    fun allAnalyses(): List<StockAnalysis2> {
        val searchResponse = restHighLevelClient.search(
            SearchRequest(indexName).source(
                SearchSourceBuilder.searchSource().query(
                    QueryBuilders.matchAllQuery()
                )
            ), RequestOptions.DEFAULT
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
        val boolQuery = QueryBuilders.boolQuery()
        
        if (userId != null) {
            boolQuery.must(QueryBuilders.termQuery(StockAnalysisProjection::userId.name.keyword, userId))
        }
        
        if (cik != null) {
            boolQuery.must(QueryBuilders.termQuery(StockAnalysisProjection::cik.name.keyword, cik))
        }
        
        if (ticker != null) {
            boolQuery.must(QueryBuilders.termQuery(StockAnalysisProjection::ticker.name.keyword, ticker))
        }
        
        if (published != null) {
            boolQuery.must(QueryBuilders.termQuery(StockAnalysisProjection::published.name, published))
        }
        
        if (term != null) {
            boolQuery.should(QueryBuilders.matchQuery(StockAnalysisProjection::ticker.name, term))
            boolQuery.should(QueryBuilders.matchQuery(StockAnalysisProjection::name.name, term))
            boolQuery.should(QueryBuilders.matchQuery(StockAnalysisProjection::cik.name, term))
        }

        if (tags != null && tags.isNotEmpty()) {
            boolQuery.must(QueryBuilders.termsQuery(StockAnalysisProjection::tags.name.keyword, tags))
        }
        
        val searchSourceBuilder = SearchSourceBuilder
            .searchSource()
            .query(boolQuery)
        
        if (skip != null) {
            searchSourceBuilder.from(skip)
        }
        
        if (limit != null) {
            searchSourceBuilder.size(limit)
        }
        
        val searchRequest = SearchRequest(indexName)
            .source(searchSourceBuilder)
        val searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT)
        val stockAnalyses = searchResponse.hits.map { searchHit -> 
            objectMapper
                .readValue<StockAnalysisProjection>(searchHit.sourceAsString)
                .copy(_id = searchHit.id)
        }
        return FindStockAnalysisResponse(stockAnalyses)
    }

    fun publish(id: String): StockAnalysis2 {
        val stockAnalysis = getStockAnalysis(id).copy(published = true, lastUpdated = Instant.now())
        saveStockAnalysis(stockAnalysis)
        return stockAnalysis
    }

    fun unpublish(id: String): StockAnalysis2 {
        val stockAnalysis = getStockAnalysis(id).copy(published = false, lastUpdated = Instant.now())
        saveStockAnalysis(stockAnalysis)
        return stockAnalysis
    }
    private val String.keyword: String
        get() = "${this}.keyword"
}
