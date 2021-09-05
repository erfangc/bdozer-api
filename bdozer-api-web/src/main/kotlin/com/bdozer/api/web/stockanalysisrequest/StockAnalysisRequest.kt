package com.bdozer.api.web.stockanalysisrequest

import java.time.Instant
import java.util.*

data class StockAnalysisRequest(
    val _id: String = UUID.randomUUID().toString(),
    val email: String,
    val ticker: String,
    val timestamp: Instant = Instant.now(),
)
