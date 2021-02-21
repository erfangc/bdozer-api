package com.starburst.starburst.models

import com.starburst.starburst.spreadsheet.Cell

data class ModelEvaluationOutput(
    val cells: List<Cell>,
    val pvOfFcf: Double,
    val targetPriceUnderExitMultipleMethod: Double,
    val targetPriceUnderPerpetuityMethod: Double,
    val pvOfTerminalValueUnderPerpetuityMethod: Double,
    val pvOfTerminalValueUnderExitMultipleMethod: Double
)
