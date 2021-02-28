package com.starburst.starburst.xbrl.dataclasses

data class ElementDefinition(
    val namespace: String = "us-gaap",
    val name: String,
    val periodType: String,
    val type: String,
)
