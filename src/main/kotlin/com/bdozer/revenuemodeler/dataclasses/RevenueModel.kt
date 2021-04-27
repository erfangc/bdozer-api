package com.bdozer.revenuemodeler.dataclasses

data class RevenueModel(
    val _id: String,
    val stockAnalysisId: String,
    val component1: RevenueComponent,
    val operator: Operator,
    val component2: RevenueComponent,
)