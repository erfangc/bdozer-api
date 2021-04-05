package com.starburst.starburst.stockanalyzer.dataclasses

import com.starburst.starburst.spreadsheet.Cell

data class Waterfall(
    val revenue: Cell,
    val expenses: List<Cell>,
    val profit: Cell,
)