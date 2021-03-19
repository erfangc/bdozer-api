package com.starburst.starburst.edgar.factbase.ingestor.dataclasses

import com.starburst.starburst.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import java.time.Instant
import java.time.LocalDate

data class FilingCalculations(
    val cik: String,
    val adsh: String,
    val formType: String,
    val documentFiscalPeriodFocus: DocumentFiscalPeriodFocus,
    val documentFiscalYearFocus: Int,
    val documentPeriodEndDate: LocalDate,
    val incomeStatement: List<SectionNode>,
    val cashFlowStatement: List<SectionNode>,
    val balanceSheet: List<SectionNode>,
    val lastUpdated: String = Instant.now().toString(),
)