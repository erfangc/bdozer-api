package com.bdozer.api.web.stockanalysis.dataclasses

import com.bdozer.api.web.models.dataclasses.Model
import com.bdozer.api.web.spreadsheet.Cell

data class EvaluateModelResponse(
    val model: Model,
    val cells: List<Cell>,
    val derivedStockAnalytics: DerivedStockAnalytics,
)