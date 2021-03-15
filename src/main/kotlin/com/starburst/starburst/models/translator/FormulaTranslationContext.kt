package com.starburst.starburst.models.translator

import com.starburst.starburst.spreadsheet.Cell
import com.starburst.starburst.models.dataclasses.Model

data class FormulaTranslationContext(
    val cells: List<Cell>,
    val model: Model
)
