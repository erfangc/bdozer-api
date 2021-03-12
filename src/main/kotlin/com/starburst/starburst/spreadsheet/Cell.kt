package com.starburst.starburst.spreadsheet

import com.starburst.starburst.models.dataclasses.Item

data class Cell(
    val period: Int,
    val name: String,
    val item: Item,
    val value: Double? = null,
    val formula: String? = null,
    val excelFormula: String? = null,
    val address: Address? = null,
    val dependentCellNames: List<String> = emptyList()
)

