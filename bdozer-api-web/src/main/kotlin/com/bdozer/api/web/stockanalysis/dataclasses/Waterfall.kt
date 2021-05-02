package com.bdozer.api.web.stockanalysis.dataclasses

import com.bdozer.api.web.spreadsheet.Cell

data class Waterfall(
    val revenue: Cell,
    val expenses: List<Cell>,
    val profit: Cell,
)