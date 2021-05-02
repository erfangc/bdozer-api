package com.bdozer.api.web.models.translator

import com.bdozer.api.web.models.dataclasses.Model
import com.bdozer.api.web.spreadsheet.Cell

data class FormulaTranslationContext(
    val cells: List<Cell>,
    val model: Model
)
