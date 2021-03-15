package com.starburst.starburst.zacks.modelbuilder.keyinputs

data class KeyInput(
    val label: String,
    val name: String,
    val description: String?,
    val defaultValue: String
)