package com.bdozer.api.common.dataclasses.sec

import java.time.Instant
import java.time.LocalDate

data class FilingArcs(
    val _id: String,
    val cik: String,
    val adsh: String,
    val formType: String,

    val documentFiscalPeriodFocus: DocumentFiscalPeriodFocus,
    val documentFiscalYearFocus: Int,
    val documentPeriodEndDate: LocalDate,

    val incomeStatement: List<Arc> = emptyList(),
    val cashFlowStatement: List<Arc> = emptyList(),
    val balanceSheet: List<Arc> = emptyList(),

    val lastUpdated: String = Instant.now().toString(),
)

