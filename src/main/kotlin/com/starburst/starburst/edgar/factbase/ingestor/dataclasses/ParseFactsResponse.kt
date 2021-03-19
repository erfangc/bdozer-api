package com.starburst.starburst.edgar.factbase.ingestor.dataclasses

import com.starburst.starburst.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import java.time.LocalDate

data class ParseFactsResponse(
    val facts: List<Fact>,
    val documentFiscalPeriodFocus: DocumentFiscalPeriodFocus,
    val documentFiscalYearFocus: Int,
    val documentPeriodEndDate: LocalDate,
)