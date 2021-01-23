package com.starburst.starburst.models

data class Period(
    val items: List<Item>,
    val actual: Boolean = false,
    val year: Int,
    val quarter: Int
)
