package com.starburst.starburst.zacks.dataclasses

data class KeyInputs(
    val _id: String,
    val keyInputs: List<KeyInput>,
    val revenueFormula: String? = null,
)