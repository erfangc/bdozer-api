package com.starburst.starburst.models

data class Cell(
    val period: Int,
    val name: String,
    val driver: Driver? = null,
    val item: Item? = null,
    val value: Double? = null,
    val expression: String? = null,
    val address: Address? = null,
    val dependentCellNames: List<String> = emptyList()
)

data class Address(
    val row: Int,
    val column: String
)
