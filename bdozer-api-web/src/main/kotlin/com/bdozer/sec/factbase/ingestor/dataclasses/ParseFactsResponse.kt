package com.bdozer.sec.factbase.ingestor.dataclasses

import com.bdozer.sec.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.bdozer.sec.factbase.dataclasses.Fact
import java.time.LocalDate

data class ParseFactsResponse(
    val facts: List<Fact>,
    val documentFiscalPeriodFocus: DocumentFiscalPeriodFocus,
    val documentFiscalYearFocus: Int,
    val documentPeriodEndDate: LocalDate,
)