package co.bdozer.libraries.tenk.models

import java.time.Instant
import java.time.LocalDate

data class TenK(
    val id: String,
    val cik: String? = null,
    val ash: String? = null,
    val text: String,
    val url: String,
    val seqNo: Int,
    val reportDate: LocalDate,
    val accessedTime: Instant = Instant.now(),
    val section: String? = null,
    val companyName: String,
    val ticker: String? = null,
)