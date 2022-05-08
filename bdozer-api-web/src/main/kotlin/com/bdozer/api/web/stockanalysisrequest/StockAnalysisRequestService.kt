package com.bdozer.api.web.stockanalysisrequest

import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType
import org.springframework.stereotype.Service

@Service
class StockAnalysisRequestService(
    private val restHighLevelClient: RestHighLevelClient,
    private val objectMapper: ObjectMapper,
) {
    fun saveStockAnalysisRequest(ticker: String, email: String) {
        val stockAnalysisRequest = StockAnalysisRequest(ticker = ticker, email = email)
        restHighLevelClient.index(
            IndexRequest("stock-analysis-request")
                .id(stockAnalysisRequest._id)
                .source(objectMapper.writeValueAsString(stockAnalysisRequest), XContentType.JSON),
            RequestOptions.DEFAULT
        )
    }
}