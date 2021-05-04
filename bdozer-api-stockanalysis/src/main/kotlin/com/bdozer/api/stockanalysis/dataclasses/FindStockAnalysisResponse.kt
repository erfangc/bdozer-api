package com.bdozer.api.stockanalysis.dataclasses

data class FindStockAnalysisResponse(
    val totalCount: Int = 0,
    val stockAnalyses: List<StockAnalysisProjection>,
)