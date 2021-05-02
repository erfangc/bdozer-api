package com.bdozer.revenuemodeler.dataclasses

import com.bdozer.models.dataclasses.Model

data class ModelRevenueRequest(
    val revenueModel: RevenueModel,
    val model: Model,
)