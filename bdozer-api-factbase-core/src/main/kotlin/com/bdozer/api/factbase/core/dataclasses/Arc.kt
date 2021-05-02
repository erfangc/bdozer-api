package com.bdozer.api.factbase.core.dataclasses

data class Arc(
    val parentHref: String? = null,
    val conceptHref: String,
    val conceptName: String,
    val preferredLabel: String? = null,
    val calculations: List<Calculation> = emptyList()
)