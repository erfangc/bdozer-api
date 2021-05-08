package com.bdozer.api.stockanalysis.dataclasses

data class FindStockAnalysisResponse(
    val stockAnalyses: List<StockAnalysisProjection>,
)