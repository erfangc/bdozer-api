package com.bdozer.api.factbase.core.support.dataclasses

import com.bdozer.api.factbase.core.dataclasses.DocumentFiscalPeriodFocus
import com.bdozer.api.factbase.core.dataclasses.Fact
import java.time.LocalDate

data class ParseFactsResponse(
    val facts: List<Fact>,
    val documentFiscalPeriodFocus: DocumentFiscalPeriodFocus,
    val documentFiscalYearFocus: Int,
    val documentPeriodEndDate: LocalDate,
)