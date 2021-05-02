package com.bdozer.api.factbase.core.dataclasses

data class Dimension(
    val dimensionConcept: String,
    val memberConcepts: Set<String>,
)