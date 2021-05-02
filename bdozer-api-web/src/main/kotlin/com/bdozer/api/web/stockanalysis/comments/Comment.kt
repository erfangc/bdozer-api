package com.bdozer.api.web.stockanalysis.comments

import java.time.Instant
import java.util.*

data class Comment(
    val _id: String = UUID.randomUUID().toString(),
    val stockAnalysisId: String,
    val text: String,
    val lastUpdated: Instant = Instant.now(),
    val userId: String? = null,
    val name: String? = null,
)