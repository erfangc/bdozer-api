package com.bdozer.api.web.models

import bdozer.api.common.model.Model
import bdozer.api.common.spreadsheet.Cell

data class EvaluateModelResult(
    val model: Model,
    val cells: List<Cell>,
    val targetPrice: Double
)