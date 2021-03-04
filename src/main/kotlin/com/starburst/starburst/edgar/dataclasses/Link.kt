package com.starburst.starburst.edgar.dataclasses

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Link(
    val local: List<String> = emptyList(),
    val remote: List<String> = emptyList()
)
