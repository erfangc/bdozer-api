package com.bdozer.api.stockanalysis.models

import com.bdozer.api.web.stockanalysis.models.StockAnalysisProjection

data class FindStockAnalysisResponse(
    val stockAnalyses: List<StockAnalysisProjection>,
)