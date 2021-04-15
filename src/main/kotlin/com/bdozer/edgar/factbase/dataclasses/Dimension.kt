package com.bdozer.edgar.factbase.dataclasses

data class Dimension(
    val dimensionConcept: String,
    val memberConcepts: Set<String>,
)