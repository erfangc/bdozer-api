package com.starburst.starburst.models.evaluator

import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.spreadsheet.Cell

data class EvaluateModelResult(
    val model: Model,
    val cells: List<Cell>,
    val targetPrice: Double
)