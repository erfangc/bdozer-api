package com.bdozer.stockanalysis.dataclasses

import com.bdozer.models.dataclasses.Model
import com.bdozer.spreadsheet.Cell

data class EvaluateModelResponse(
    val model: Model,
    val cells: List<Cell>,
    val derivedStockAnalytics: DerivedStockAnalytics,
)