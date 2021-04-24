package com.bdozer.sec.factbase.ingestor.rss

import java.time.LocalDate

data class XbrlRssItem(
    val _id: String,
    val companyName: String? = null,
    val formType: String? = null,
    val cikNumber: String,
    val accessionNumber: String,
    val period: LocalDate? = null,
    val status: Status,
    val message: String? = null,
)