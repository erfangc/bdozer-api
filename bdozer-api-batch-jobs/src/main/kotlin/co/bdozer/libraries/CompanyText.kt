package co.bdozer.libraries

import java.time.Instant

data class CompanyText(
    val id: String,
    val ticker: String,
    val text: String,
    val accessionTime: Instant = Instant.now(),
    val url: String,
    val source: String,
    val metaData: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList(),
)
