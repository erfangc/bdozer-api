package com.starburst.starburst.marketing

import java.time.Instant
import java.util.*

data class StockAnalysisRequest(
    val _id: String = UUID.randomUUID().toString(),
    val cik: String,
    val ticker: String,
    val lastUpdated: Instant = Instant.now(),
)