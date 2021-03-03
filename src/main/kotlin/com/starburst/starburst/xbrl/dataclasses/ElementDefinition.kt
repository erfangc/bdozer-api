package com.starburst.starburst.xbrl.dataclasses

data class ElementDefinition(
    val id: String,
    val namespace: String,
    val name: String,
    val periodType: String,
    val type: String,
)
