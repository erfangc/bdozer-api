package com.starburst.starburst.models

data class Item(
    val name: String,
    val description: String? = null,
    val value: Double,
    val contributors: List<Contributor> = emptyList(),
    val segment: String? = null,
    val geography: String? = null,
    val resource: String? = null,
    val type: Type
)
