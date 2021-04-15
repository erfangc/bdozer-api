package com.bdozer.models.dataclasses

import com.bdozer.spreadsheet.Cell

data class ModelEvaluationOutput(
    val cells: List<Cell>,
    val pvOfFcf: Double,
    val terminalFcf: Double,
    val targetPriceUnderExitMultipleMethod: Double,
    val targetPriceUnderPerpetuityMethod: Double,
    val pvOfTerminalValueUnderPerpetuityMethod: Double,
    val pvOfTerminalValueUnderExitMultipleMethod: Double
)