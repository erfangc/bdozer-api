package com.bdozer.api.factbase.core.dataclasses

import java.time.Instant
import java.time.LocalDate

data class ProcessedSECFiling(
    val _id: String,
    val cik: String,
    val adsh: String,
    val numberOfFactsFound: Int? = null,
    val error: String? = null,
    val documentFiscalPeriodFocus: DocumentFiscalPeriodFocus? = null,
    val documentFiscalYearFocus: Int? = null,
    val documentPeriodEndDate: LocalDate? = null,
    val timestamp: Instant,
)