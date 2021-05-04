package com.bdozer.api.web.revenuemodeler.dataclasses

import com.bdozer.api.models.dataclasses.Model

data class ModelRevenueRequest(
    val revenueModel: RevenueModel,
    val model: Model,
)