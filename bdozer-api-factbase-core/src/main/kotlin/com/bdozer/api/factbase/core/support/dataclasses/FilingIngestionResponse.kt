package com.bdozer.api.factbase.core.support.dataclasses

import com.bdozer.api.factbase.core.dataclasses.DocumentFiscalPeriodFocus
import java.time.LocalDate

data class FilingIngestionResponse(
    val documentFiscalPeriodFocus: DocumentFiscalPeriodFocus,
    val documentFiscalYearFocus: Int,
    val documentPeriodEndDate: LocalDate,
    val numberOfFactsFound: Int,
)