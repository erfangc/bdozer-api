package com.starburst.starburst.xbrl.dataclasses

data class Link(
    val local: List<String> = emptyList(),
    val remote: List<String> = emptyList()
)
