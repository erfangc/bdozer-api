package com.starburst.starburst.models

data class Item(
    val name: String,
    val historicalValue: Double = 0.0,
    val drivers: List<Driver>? = emptyList(),
    val expression: String? = null
)
