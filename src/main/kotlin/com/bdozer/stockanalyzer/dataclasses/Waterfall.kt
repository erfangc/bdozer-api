package com.bdozer.stockanalyzer.dataclasses

import com.bdozer.spreadsheet.Cell

data class Waterfall(
    val revenue: Cell,
    val expenses: List<Cell>,
    val profit: Cell,
)