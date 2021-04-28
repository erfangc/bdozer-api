package com.bdozer.revenuemodeler.dataclasses

data class RevenueModel(
    val _id: String,
    val stockAnalysisId: String,
    val drivers: List<RevenueDriver>,
)
