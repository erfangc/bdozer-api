package com.bdozer.api.web.stockanalysis.models

import java.time.Instant

data class StockAnalysisProjection(
    val _id: String? = null,
    val userId: String? = null,
    val name: String? = null,
    val description: String? = null,
    val cik: String? = null,
    val ticker: String? = null,
    val currentPrice: Double? = null,
    val targetPrice: Double? = null,
    val finalPrice: Double? = null,
    val percentUpside: Double? = null,
    val published: Boolean? = null,
    val lastUpdated: Instant? = null,
    val tags: List<String> = emptyList(),
    val zacksDerivedAnalytics: ZacksDerivedAnalytics = ZacksDerivedAnalytics(),
)