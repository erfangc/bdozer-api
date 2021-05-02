package com.bdozer.api.web.models

import com.bdozer.api.web.models.dataclasses.Model
import com.bdozer.api.web.spreadsheet.Cell

data class EvaluateModelResult(
    val model: Model,
    val cells: List<Cell>,
    val targetPrice: Double
)