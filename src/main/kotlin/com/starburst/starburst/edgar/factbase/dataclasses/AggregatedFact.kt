package com.starburst.starburst.edgar.factbase.dataclasses

import java.time.LocalDate

data class AggregatedFact(
    val factIds: List<String>,
    val value: Double = 0.0,
    val conceptName: String,
    val documentFiscalPeriodFocus: DocumentFiscalPeriodFocus?,
    val documentPeriodEndDate: LocalDate,
)