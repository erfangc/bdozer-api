package com.starburst.starburst.stockanalyzer.dataclasses

data class Dimension(
    val dimensionConcept: String,
    val memberConcepts: Set<String>,
)