package bdozer.api.common.stockanalysis

import bdozer.api.common.spreadsheet.Cell

data class Waterfall(
    val revenue: Cell,
    val expenses: List<Cell>,
    val profit: Cell,
)