package com.starburst.starburst.edgar.dataclasses

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MetaLink(
    val instance: Map<String, Instance> = emptyMap()
)

