package com.bdozer.stockanalysis.dataclasses

import com.bdozer.spreadsheet.Cell

data class EvaluateModelResponse(
    val cells: List<Cell>,
    val derivedStockAnalytics: DerivedStockAnalytics,
)