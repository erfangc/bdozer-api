package co.bdozer.libraries.master.models

data class LatestMetrics(
    val revenue: Double?,
    val ebitda: Double?,
    val ebit: Double?,
    val netIncome: Double?,
    val debtToEquity: Double?,
    val debtToAsset: Double?,
    val totalAsset: Double?,
    val totalLiability: Double?,
    val longTermDebt: Double?,
    val longTermDebtToAsset: Double?,
)