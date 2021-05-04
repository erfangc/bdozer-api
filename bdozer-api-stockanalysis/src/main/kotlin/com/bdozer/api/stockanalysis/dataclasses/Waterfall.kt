package com.bdozer.api.stockanalysis.dataclasses

import com.bdozer.api.models.dataclasses.spreadsheet.Cell

data class Waterfall(
    val revenue: Cell,
    val expenses: List<Cell>,
    val profit: Cell,
)