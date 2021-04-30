package com.bdozer.revenuemodeler.dataclasses

data class RevenueModel(
    val _id: String,
    val revenueDriverType: RevenueDriverType? = null,
    val stockAnalysisId: String,

    /**
     *
     */
    val terminalYear: Int? = null,
    val terminalYearAverageRevenuePerUser: Double? = null,
    val terminalYearActiveUser: Double? = null,
)
