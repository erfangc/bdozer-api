package com.bdozer.api.web.controlleradvice

import java.time.Instant
import java.util.*

data class ApiError(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val timestamp: Instant = Instant.now()
)