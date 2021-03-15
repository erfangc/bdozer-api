package com.starburst.starburst.edgar.factbase.ingestor.dataclasses

data class FilingIngestionResponse(
    val documentFiscalPeriodFocus: String,
    val documentFiscalYearFocus: Int,
    val documentPeriodEndDate: String,
    val numberOfFactsFound: Int
)
