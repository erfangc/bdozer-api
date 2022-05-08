package com.bdozer.api.web.watchlist

import com.bdozer.api.web.authn.UserProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("/api/watch-lists")
class WatchListsController(
    private val userProvider: UserProvider,
    private val objectMapper: ObjectMapper,
    private val restHighLevelClient: RestHighLevelClient,
) {

    private val indexName = "watch-lists"

    @GetMapping
    fun getWatchedStockAnalyses(): GetWatchedStockAnalysesResponse? {
        val userEmail = getUserEmail()
        val getResponse = restHighLevelClient.get(
            GetRequest(indexName).id(userEmail),
            RequestOptions.DEFAULT
        )
        val watchList = if (getResponse.isExists) {
            objectMapper.readValue(getResponse.sourceAsBytes)
        } else {
            WatchList(
                _id = userEmail,
                stockAnalysisIds = emptyList(),
            )
        }
        return GetWatchedStockAnalysesResponse(
            watchList = watchList,
            stockAnalyses = emptyList(),
        )
    }

    @GetMapping("{stockAnalysisId}")
    fun isWatching(@PathVariable stockAnalysisId: String): Boolean {
        return false
    }

    private fun getUserEmail(): String {
        val userInfo = userProvider.get()
        return userInfo.values["email"]?.toString()
            ?: error("Unable to determine the current user's email")
    }

    @PostMapping
    fun watch(@RequestParam stockAnalysisId: String): WatchList {
        val userEmail = getUserEmail()
        val getResponse = restHighLevelClient.get(
            GetRequest(indexName).id(userEmail),
            RequestOptions.DEFAULT
        )
        val watchList = if (getResponse.isExists) {
            objectMapper.readValue(getResponse.sourceAsBytes)
        } else {
            WatchList(
                _id = userEmail,
                stockAnalysisIds = emptyList(),
            )
        }

        val watchList1 = watchList.copy(stockAnalysisIds = watchList.stockAnalysisIds + stockAnalysisId)
        restHighLevelClient.index(
            IndexRequest(indexName)
                .id(userEmail)
                .source(objectMapper.writeValueAsString(watchList1)),
            RequestOptions.DEFAULT
        )
        return watchList1
    }

    @DeleteMapping
    fun unwatch(@RequestParam stockAnalysisId: String): WatchList {
        val userEmail = getUserEmail()
        val getResponse = restHighLevelClient.get(
            GetRequest(indexName).id(userEmail),
            RequestOptions.DEFAULT
        )
        val watchList = if (getResponse.isExists) {
            objectMapper.readValue(getResponse.sourceAsBytes)
        } else {
            WatchList(
                _id = userEmail,
                stockAnalysisIds = emptyList(),
            )
        }

        val watchList1 = watchList.copy(stockAnalysisIds = watchList.stockAnalysisIds - stockAnalysisId)
        restHighLevelClient.index(
            IndexRequest(indexName)
                .id(userEmail)
                .source(objectMapper.writeValueAsString(watchList1)),
            RequestOptions.DEFAULT
        )
        return watchList1
    }

}