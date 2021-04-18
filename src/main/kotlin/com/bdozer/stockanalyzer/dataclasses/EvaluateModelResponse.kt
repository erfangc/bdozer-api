package com.bdozer.stockanalyzer.dataclasses

import com.bdozer.spreadsheet.Cell

data class EvaluateModelResponse(
    val cells: List<Cell>,
    val derivedStockAnalytics: DerivedStockAnalytics,
)