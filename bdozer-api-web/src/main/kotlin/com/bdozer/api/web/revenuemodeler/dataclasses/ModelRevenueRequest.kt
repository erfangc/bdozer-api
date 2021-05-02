package com.bdozer.api.web.revenuemodeler.dataclasses

import com.bdozer.api.web.models.dataclasses.Model

data class ModelRevenueRequest(
    val revenueModel: RevenueModel,
    val model: Model,
)