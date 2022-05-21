package com.bdozer.api.models.dataclasses

import java.time.Instant

data class BuildZacksModelResponse(
    val id: String,
    val cik: String? = null,
    val ticker:String,
    val status: Int = 200,
    val message: String? = null,
    val targetPrice: Double? = null,
    val finalPrice: Double? = null,
    val timestamp: Instant = Instant.now(),
)