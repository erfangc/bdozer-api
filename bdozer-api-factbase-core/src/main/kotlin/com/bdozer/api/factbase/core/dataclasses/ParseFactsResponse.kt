package com.bdozer.api.factbase.core.dataclasses

import com.bdozer.api.common.dataclasses.sec.DocumentFiscalPeriodFocus
import com.bdozer.api.common.dataclasses.sec.Fact
import java.time.LocalDate

data class ParseFactsResponse(
    val facts: List<Fact>,
    val documentFiscalPeriodFocus: DocumentFiscalPeriodFocus,
    val documentFiscalYearFocus: Int,
    val documentPeriodEndDate: LocalDate,
)