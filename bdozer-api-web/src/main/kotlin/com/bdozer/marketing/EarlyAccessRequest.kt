package com.bdozer.marketing

import java.time.Instant
import java.util.*

data class EarlyAccessRequest(
    val _id: String = UUID.randomUUID().toString(),
    val email: String,
    val lastUpdated: Instant = Instant.now(),
)
