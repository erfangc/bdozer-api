package com.bdozer.api.stockanalysis.dataclasses

import com.bdozer.api.models.dataclasses.Model

data class EvaluateModelRequest(
    val existingAnalysis: StockAnalysis2? = null,
    val model: Model,
)