package com.bdozer.stockanalyzer.dataclasses

data class FindStockAnalysisResponse(
    val totalCount: Int = 0,
    val stockAnalyses: List<StockAnalysis2>,
)