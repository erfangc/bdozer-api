package com.starburst.starburst.edgar.factbase.dataclasses

import com.starburst.starburst.edgar.factbase.ingestor.dataclasses.Arc
import java.time.Instant
import java.time.LocalDate

data class FilingCalculations(
    val _id: String,
    val cik: String,
    val adsh: String,
    val formType: String,

    val documentFiscalPeriodFocus: DocumentFiscalPeriodFocus,
    val documentFiscalYearFocus: Int,
    val documentPeriodEndDate: LocalDate,

    val incomeStatement: List<Arc>,
    val cashFlowStatement: List<Arc>,
    val balanceSheet: List<Arc>,

    /**
     * Crucial Item / concept names
     */
    val conceptNames: ConceptNames,

    val lastUpdated: String = Instant.now().toString(),
)

