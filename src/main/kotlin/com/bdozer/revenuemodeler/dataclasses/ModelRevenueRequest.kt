package com.bdozer.revenuemodeler.dataclasses

import com.bdozer.models.dataclasses.Model
import com.bdozer.revenuemodeler.dataclasses.RevenueModel

data class ModelRevenueRequest(
    val revenueModel: RevenueModel,
    val model: Model,
)