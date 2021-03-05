package com.starburst.starburst.edgar.dataclasses

data class ElementDefinition(
    val id: String,
    val longNamespace: String,
    val name: String,
    val periodType: String,
    val type: String,
)
