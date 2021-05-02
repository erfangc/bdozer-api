package com.bdozer.sec.factbase.ingestor.dataclasses

import com.bdozer.sec.factbase.dataclasses.DocumentFiscalPeriodFocus
import java.time.LocalDate

data class FilingIngestionResponse(
    val documentFiscalPeriodFocus: DocumentFiscalPeriodFocus,
    val documentFiscalYearFocus: Int,
    val documentPeriodEndDate: LocalDate,
    val numberOfFactsFound: Int
)
