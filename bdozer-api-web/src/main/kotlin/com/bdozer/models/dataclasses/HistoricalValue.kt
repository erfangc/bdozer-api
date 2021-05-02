package com.bdozer.models.dataclasses

data class HistoricalValue(
    /**
     * [factId] is the single [Fact] that underlies this historical value
     * instance
     */
    val factId: String? = null,

    /**
     * [factId] is the series of decomposed [Fact]s that underlies this historical value
     * instance along some dimension
     */
    val factIds: List<String> = emptyList(),
    val conceptName: String? = null,
    val documentFiscalPeriodFocus: String? = null,
    val documentFiscalYearFocus: Int? = null,
    val documentPeriodEndDate: String? = null,
    val value: Double? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val instant: String? = null,
)