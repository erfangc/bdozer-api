package com.bdozer.api.common.dataclasses.sec

data class Dimension(
    val dimensionConcept: String,
    val memberConcepts: Set<String>,
)