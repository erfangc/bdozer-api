package com.starburst.starburst.edgar.dataclasses

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DTS(
    val calculationLink: Link = Link(),
    val definitionLink: Link = Link(),
    val labelLink: Link = Link(),
    val presentationLink: Link = Link(),
    val inline: Link = Link(),
    val schema: Link = Link(),
)
