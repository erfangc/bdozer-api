package com.starburst.starburst.modelbuilder.common

import com.starburst.starburst.spreadsheet.Cell

data class Waterfall(
    val revenue: Cell,
    val topExpenses: List<Cell>,
    val profit: Cell,
)