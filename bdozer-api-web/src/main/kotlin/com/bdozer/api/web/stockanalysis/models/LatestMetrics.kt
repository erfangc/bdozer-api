package com.bdozer.api.stockanalysis.models

data class LatestMetrics(
    val revenue: Double? = null,
    val ebitda: Double? = null,
    val ebit: Double? = null,
    val netIncome: Double? = null,
    val grossMargin: Double? = null,
    val ebitdaMargin: Double? = null,
    val ebitMargin: Double? = null,
    val daMargin: Double? = null,
    val debtToEquity: Double? = null,
    val debtToAsset: Double? = null,
    val totalAsset: Double? = null,
    val totalLiability: Double? = null,
    val longTermDebt: Double? = null,
    val longTermDebtToAsset: Double? = null,
)