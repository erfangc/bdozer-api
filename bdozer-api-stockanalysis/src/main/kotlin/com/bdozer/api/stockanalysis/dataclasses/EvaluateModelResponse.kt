package com.bdozer.api.stockanalysis.dataclasses

import com.bdozer.api.models.dataclasses.Model
import com.bdozer.api.models.dataclasses.spreadsheet.Cell

data class EvaluateModelResponse(
    val model: Model,
    val cells: List<Cell>,
    val derivedStockAnalytics: DerivedStockAnalytics,
)