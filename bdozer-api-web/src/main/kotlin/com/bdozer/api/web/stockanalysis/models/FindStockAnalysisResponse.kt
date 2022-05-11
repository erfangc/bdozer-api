package com.bdozer.api.stockanalysis.models

data class FindStockAnalysisResponse(
    val stockAnalyses: List<StockAnalysisProjection>,
)