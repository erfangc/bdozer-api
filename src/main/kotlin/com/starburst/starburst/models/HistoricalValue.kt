package com.starburst.starburst.models

data class HistoricalValue(
    val factId: String? = null,
    val value: Double? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val instant: String? = null,
)