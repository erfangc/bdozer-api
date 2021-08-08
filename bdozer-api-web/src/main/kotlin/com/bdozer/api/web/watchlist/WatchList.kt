package com.bdozer.api.web.watchlist

import java.time.Instant

data class WatchList(
    // this will be the user email
    val _id: String,
    val stockAnalysisIds: List<String> = emptyList(),
    val lastUpdated: Instant = Instant.now(),
)