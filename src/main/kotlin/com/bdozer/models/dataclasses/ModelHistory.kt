package com.bdozer.models.dataclasses

import java.util.*

data class ModelHistory(
    val _id: String = UUID.randomUUID().toString(),
    val modelId: String,
    val model: Model,
    val changeSummary: String? = null
)