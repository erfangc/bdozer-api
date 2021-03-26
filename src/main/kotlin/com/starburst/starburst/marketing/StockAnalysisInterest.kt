package com.starburst.starburst.marketing

import java.time.Instant
import java.util.*

data class StockAnalysisInterest(
    val _id: String = UUID.randomUUID().toString(),
    val email: String,
    val requests: List<StockAnalysisRequest>,
    val lastUpdated: Instant = Instant.now(),
)
