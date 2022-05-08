package com.bdozer.api.web.watchlist

import com.bdozer.api.web.authn.UserProvider
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.web.bind.annotation.*

// TODO finish this for Elasticsearch
@RestController
@CrossOrigin
@RequestMapping("/api/watch-lists")
class WatchListsController(
    private val userProvider: UserProvider,
    private val restHighLevelClient: RestHighLevelClient,
) {

    private val indexName = "watch-lists"

    @GetMapping
    fun getWatchedStockAnalyses(): GetWatchedStockAnalysesResponse? {
        val email = getUserEmail()
        return GetWatchedStockAnalysesResponse(
            watchList = WatchList(_id = email, emptyList()),
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
        TODO()
    }

    @DeleteMapping
    fun unwatch(@RequestParam stockAnalysisId: String): WatchList {
        TODO()
    }

}