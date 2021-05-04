package com.bdozer.api.models.dataclasses

import com.bdozer.api.models.dataclasses.spreadsheet.Cell

data class EvaluateModelResult(
    val model: Model,
    val cells: List<Cell>,
    val targetPrice: Double
)