package bdozer.api.common.model

import bdozer.api.common.spreadsheet.Cell

data class EvaluateModelResult(
    val model: Model,
    val cells: List<Cell>,
    val targetPrice: Double
)