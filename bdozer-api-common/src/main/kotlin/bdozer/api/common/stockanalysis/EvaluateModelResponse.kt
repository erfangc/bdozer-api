package bdozer.api.common.stockanalysis

import bdozer.api.common.model.Model
import bdozer.api.common.spreadsheet.Cell

data class EvaluateModelResponse(
    val model: Model,
    val cells: List<Cell>,
    val derivedStockAnalytics: DerivedStockAnalytics,
)