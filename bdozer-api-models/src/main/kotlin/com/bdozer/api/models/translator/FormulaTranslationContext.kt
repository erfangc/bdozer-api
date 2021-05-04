package com.bdozer.api.models.translator

import com.bdozer.api.models.dataclasses.Model
import com.bdozer.api.models.dataclasses.spreadsheet.Cell

data class FormulaTranslationContext(
    val cells: List<Cell>,
    val model: Model
)
