package com.starburst.starburst.models.translator

import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.spreadsheet.Cell

data class FormulaTranslationContext(
    val cells: List<Cell>,
    val model: Model
)
