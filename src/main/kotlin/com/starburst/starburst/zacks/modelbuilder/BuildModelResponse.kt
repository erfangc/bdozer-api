package com.starburst.starburst.zacks.modelbuilder

import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.spreadsheet.Cell

data class BuildModelResponse(
    val model: Model,
    val targetPrice: Double,
    val cells: List<Cell>,
    val excel: ByteArray,
)