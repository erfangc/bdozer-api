package com.bdozer.api.stockanalysis.dataclasses

data class PriceUpdateRequest(
    val stockAnalysisId: String,
    val ticker: String,
)