package com.bdozer.marketing

import java.time.Instant
import java.util.*

data class Feedback(
    val _id: String = UUID.randomUUID().toString(),
    val body: Any,
    val lastUpdated: Instant = Instant.now(),
    val version: String = "N/A",
)