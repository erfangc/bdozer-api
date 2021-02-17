package com.starburst.starburst.models

data class Model(
    val items: List<Item> = emptyList(),
    val periods: Int? = null
)
