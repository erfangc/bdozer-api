package com.starburst.starburst.xbrl

data class MetaLink(
    val instance: Map<String, Instance> = emptyMap()
)

data class Instance(
    val dts: DTS = DTS()
)

data class DTS(
    val calculationLink: Link = Link(),
    val definitionLink: Link = Link(),
    val labelLink: Link = Link(),
    val presentationLink: Link = Link(),
    val inline: Link = Link(),
    val schema: Link = Link(),
)

data class Link(
    val local: List<String> = emptyList(),
    val remote: List<String> = emptyList()
)
