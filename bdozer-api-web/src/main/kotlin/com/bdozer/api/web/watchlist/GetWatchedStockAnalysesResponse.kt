package com.bdozer.api.web.watchlist

import com.bdozer.api.stockanalysis.models.StockAnalysis2

data class GetWatchedStockAnalysesResponse(
    val watchList: WatchList,
    val stockAnalyses: List<StockAnalysis2>,
)