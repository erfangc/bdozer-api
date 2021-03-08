package com.starburst.starburst.models

data class HistoricalValues(
    val fiscalYear: List<HistoricalValue> = emptyList(),
    val quarterly: List<HistoricalValue> = emptyList(),
    val ltm: HistoricalValue? = null
)