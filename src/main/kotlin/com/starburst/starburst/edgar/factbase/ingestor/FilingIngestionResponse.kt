package com.starburst.starburst.edgar.factbase.ingestor

data class FilingIngestionResponse(
    val documentFiscalPeriodFocus: String,
    val documentFiscalYearFocus: Int,
    val documentPeriodEndDate: String,
    val numberOfFactsFound: Int
)
