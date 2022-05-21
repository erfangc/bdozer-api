package com.bdozer.api.web.zacksmodels

import java.time.Instant

data class BuildZacksModelResponse(
    val cik: String,
    val ticker:String,
    val targetPrice: Double,
    val finalPrice: Double,
    val timestamp: Instant = Instant.now(),
)