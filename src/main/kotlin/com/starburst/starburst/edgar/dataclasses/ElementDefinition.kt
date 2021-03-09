package com.starburst.starburst.edgar.dataclasses

data class ElementDefinition(
    val id: String,
    val longNamespace: String,
    val name: String,
    val type: String? = null,
    val abstract: Boolean? = null,
    val periodType: String? = null,
    val nillable: Boolean? = null,
    val substitutionGroup: String? = null,
    val balance: String? = null
)
