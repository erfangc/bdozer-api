package com.starburst.starburst.models.dataclasses

data class HistoricalValue(
    val factId: String? = null,
    val documentFiscalPeriodFocus: String? = null,
    val documentFiscalYearFocus: Int? = null,
    val documentPeriodEndDate: String? = null,
    val value: Double? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val instant: String? = null,
)