package com.starburst.starburst

import java.time.Instant

data class ApiError(
    val id: String,
    val message: String,
    val timestamp: Instant
)
