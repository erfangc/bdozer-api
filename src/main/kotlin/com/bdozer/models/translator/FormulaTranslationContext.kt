package com.bdozer.models.translator

import com.bdozer.models.dataclasses.Model
import com.bdozer.spreadsheet.Cell

data class FormulaTranslationContext(
    val cells: List<Cell>,
    val model: Model
)
