package co.bdozer.libraries.tenk.sectionparser

import org.jsoup.nodes.Element

data class TenKSection(
    val name: String,
    val startAnchor: String?,
    val endAnchor: String?,
    val elements: List<Element> = emptyList(),
)