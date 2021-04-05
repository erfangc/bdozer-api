package com.starburst.starburst.stockanalyzer.overrides

import com.starburst.starburst.models.dataclasses.Item

data class ModelOverride(
    val _id: String,
    val cik: String,
    val items: List<Item> = emptyList(),
)