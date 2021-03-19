package com.starburst.starburst.edgar.factbase.ingestor.dataclasses

import com.starburst.starburst.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import java.time.LocalDate

data class FilingIngestionResponse(
    val documentFiscalPeriodFocus: DocumentFiscalPeriodFocus,
    val documentFiscalYearFocus: Int,
    val documentPeriodEndDate: LocalDate,
    val numberOfFactsFound: Int
)
