package com.starburst.starburst.edgar.factbase.dataclasses

data class Calculation(
    val conceptHref: String,
    val weight: Double,
    val conceptName: String,
)