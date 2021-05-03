package com.bdozer.api.web.revenuemodeler.dataclasses

import bdozer.api.common.model.Model

data class ModelRevenueRequest(
    val revenueModel: RevenueModel,
    val model: Model,
)