package com.bdozer.sec.factbase.dataclasses

data class Dimension(
    val dimensionConcept: String,
    val memberConcepts: Set<String>,
)