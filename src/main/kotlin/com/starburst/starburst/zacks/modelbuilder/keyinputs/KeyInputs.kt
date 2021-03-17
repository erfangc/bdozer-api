package com.starburst.starburst.zacks.modelbuilder.keyinputs

data class KeyInputs(
    val _id: String,
    val keyInputs: List<KeyInput> = emptyList(),
    val formula: String,
)