package com.bdozer.revenuemodeler.dataclasses

data class RevenueModel(
    val _id: String,
    val revenueDriverType: RevenueDriverType? = null,
    val enabled: Boolean = false,
    val stockAnalysisId: String,
    val drivers: List<RevenueDriver>,
)
