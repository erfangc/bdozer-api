package com.bdozer.edgar.factbase.ingestor.dataclasses

import com.bdozer.edgar.factbase.dataclasses.Calculation

data class Arc(
    val parentHref: String? = null,
    val conceptHref: String,
    val conceptName: String,
    val preferredLabel: String? = null,
    val calculations: List<Calculation> = emptyList()
)