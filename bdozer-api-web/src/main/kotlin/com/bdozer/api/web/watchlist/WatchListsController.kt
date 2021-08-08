package com.bdozer.api.web.watchlist

import com.bdozer.api.web.authn.UserProvider
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("/api/watch-lists")
class WatchListsController(
    private val userProvider: UserProvider,
    mongoDatabase: MongoDatabase,
) {
    private val watchLists = mongoDatabase.getCollection<WatchList>()

    @GetMapping
    fun getWatchList(): WatchList? {
        val email = getUserEmail()
        return watchLists.findOneById(email)
    }

    private fun getUserEmail(): String {
        val userInfo = userProvider.get()
        return userInfo.values["email"]?.toString()
            ?: error("Unable to determine the current user's email")
    }

    @PostMapping
    fun watch(@RequestParam stockAnalysisId: String): WatchList {
        val email = getUserEmail()
        val existing = watchLists.findOneById(email) ?: WatchList(_id = email)
        val updated = existing
            .copy(stockAnalysisIds = existing.stockAnalysisIds + stockAnalysisId)
        watchLists.save(updated)
        return updated
    }

    @DeleteMapping
    fun unwatch(@RequestParam stockAnalysisId: String): WatchList {
        val email = getUserEmail()
        val existing = watchLists.findOneById(email) ?: WatchList(_id = email)
        val updated = existing
            .copy(stockAnalysisIds = existing.stockAnalysisIds.filter { it != stockAnalysisId })
        watchLists.save(updated)
        return updated
    }

}