package com.starburst.starburst.edgar.factbase.ingestor.dataclasses

import java.time.LocalDate

data class FilingIngestionResponse(
    val documentFiscalPeriodFocus: String,
    val documentFiscalYearFocus: Int,
    val documentPeriodEndDate: LocalDate,
    val numberOfFactsFound: Int
)
