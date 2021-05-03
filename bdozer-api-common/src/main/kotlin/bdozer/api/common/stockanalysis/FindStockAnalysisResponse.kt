package bdozer.api.common.stockanalysis

data class FindStockAnalysisResponse(
    val totalCount: Int = 0,
    val stockAnalyses: List<StockAnalysisProjection>,
)