package com.starburst.starburst.edgar.factbase.ingestor.dataclasses

data class SectionNode(
    val parentHref: String? = null,
    val conceptHref: String,
    val calculations: List<Calculation> = emptyList()
)