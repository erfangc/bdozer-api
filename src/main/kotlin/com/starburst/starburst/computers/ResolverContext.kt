package com.starburst.starburst.computers

import com.starburst.starburst.spreadsheet.Cell
import com.starburst.starburst.models.Model

data class ResolverContext(
    val cells: List<Cell>,
    val model: Model
)
