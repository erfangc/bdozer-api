package com.bdozer.api.web.semanticsearch

import CompanyText
import com.bdozer.api.web.semanticsearch.models.Document
import com.bdozer.api.web.semanticsearch.models.SemanticSearchRequest
import com.bdozer.api.web.semanticsearch.models.SemanticSearchResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.BasicHttpEntity
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.stereotype.Service

@Service
class SemanticSearchService(
    private val restHighLevelClient: RestHighLevelClient,
    private val objectMapper: ObjectMapper,
    private val httpClient: HttpClient,
) {
    private val semanticSearchServerEndpoint = System
        .getenv("SEMANTIC_SEARCH_SERVER_ENDPOINT")
        ?: "http://localhost:8000"

    fun semanticSearch(ticker: String, question: String): SemanticSearchResponse {
        val searchHits = searchElastic(question, ticker)
        return semanticSearch(searchHits, question)
    }

    private fun semanticSearch(
        searchHits: SearchHits,
        question: String
    ): SemanticSearchResponse {
        // take the top 10 search hits and run it through a semantic search
        val documents = searchHits.hits.take(10).map { hit ->
            val companyText = objectMapper.readValue<CompanyText>(hit.sourceAsString)
            Document(
                id = companyText.id,
                text = companyText.text,
                metadata = mapOf(
                    "url" to companyText.url,
                    "ticker" to companyText.ticker,
                )
            )
        }
        val semanticSearchRequest = SemanticSearchRequest(question = question, documents = documents)

        val json = objectMapper.writeValueAsString(semanticSearchRequest)
        val httpPost = HttpPost("$semanticSearchServerEndpoint/semantic-search-server/api/v1/semantic-search")
        val basicHttpEntity = BasicHttpEntity()
        basicHttpEntity.setContentType("application/json")
        basicHttpEntity.content = json.byteInputStream()
        httpPost.entity = basicHttpEntity
        
         try {
            val httpResponse = httpClient.execute(httpPost)
             val body = httpResponse.entity
             return objectMapper.readValue(body.content)
        } catch (e: Exception) {
            throw e
        } finally {
            httpPost.releaseConnection()
        }
    }

    private fun searchElastic(question: String, ticker: String): SearchHits {
        val searchSource = SearchSourceBuilder.searchSource()
        val boolQuery = QueryBuilders.boolQuery()
        boolQuery.must(QueryBuilders.matchQuery("text", question))
        boolQuery.must(QueryBuilders.termQuery("${CompanyText::ticker.name}.keyword", ticker))
        searchSource.query(boolQuery)
        val searchRequest = SearchRequest("companytext")
            .source(searchSource)
        val searchResponse = restHighLevelClient.search(
            searchRequest,
            RequestOptions.DEFAULT,
        )
        return searchResponse.hits
    }
}