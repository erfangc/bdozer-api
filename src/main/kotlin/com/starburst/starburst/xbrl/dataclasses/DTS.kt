package com.starburst.starburst.xbrl.dataclasses

data class DTS(
    val calculationLink: Link = Link(),
    val definitionLink: Link = Link(),
    val labelLink: Link = Link(),
    val presentationLink: Link = Link(),
    val inline: Link = Link(),
    val schema: Link = Link(),
)
