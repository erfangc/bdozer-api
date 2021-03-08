package com.starburst.starburst.edgar.factbase.ingestor

import com.starburst.starburst.edgar.dataclasses.Fact

data class ParseFactsResponse(
    val facts: List<Fact>,
    val documentFiscalPeriodFocus: String,
    val documentFiscalYearFocus: Int,
    val documentPeriodEndDate: String,
)