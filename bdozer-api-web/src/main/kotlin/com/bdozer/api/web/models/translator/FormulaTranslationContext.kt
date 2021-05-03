package com.bdozer.api.web.models.translator

import bdozer.api.common.model.Model
import bdozer.api.common.spreadsheet.Cell

data class FormulaTranslationContext(
    val cells: List<Cell>,
    val model: Model
)
