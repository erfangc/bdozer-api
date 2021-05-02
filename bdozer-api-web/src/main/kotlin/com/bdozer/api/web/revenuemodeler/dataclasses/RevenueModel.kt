package com.bdozer.api.web.revenuemodeler.dataclasses

data class RevenueModel(
    val _id: String,
    val revenueDriverType: RevenueDriverType? = null,
    val stockAnalysisId: String,

    /**
     * properties responsible for build a business that relies on
     * average revenue per user * active user
     */
    val terminalFiscalYear: Int? = null,
    val terminalYearAverageRevenuePerUser: Double? = null,
    val terminalYearActiveUser: Double? = null,
)
