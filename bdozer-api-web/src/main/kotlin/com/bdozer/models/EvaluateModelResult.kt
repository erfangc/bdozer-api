package com.bdozer.models

import com.bdozer.models.dataclasses.Model
import com.bdozer.spreadsheet.Cell

data class EvaluateModelResult(
    val model: Model,
    val cells: List<Cell>,
    val targetPrice: Double
)