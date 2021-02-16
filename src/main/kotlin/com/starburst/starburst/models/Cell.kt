package com.starburst.starburst.models

data class Cell(
    val period: Int,
    val name: String,
    val driver: Driver,
    val value: Double? = null,
    val expression: String? = null,
    val dependencies: List<String> = emptyList()
)

