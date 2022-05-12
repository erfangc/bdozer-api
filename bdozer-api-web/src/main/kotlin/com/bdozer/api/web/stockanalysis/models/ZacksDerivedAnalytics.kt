package com.bdozer.api.web.stockanalysis.models

import com.bdozer.api.stockanalysis.models.LatestMetrics
import com.bdozer.api.stockanalysis.models.PerShareMetrics
import com.bdozer.api.stockanalysis.models.Trend
import com.bdozer.api.web.stockanalysis.support.zacks.ZacksDerivedTag

data class ZacksDerivedAnalytics(
    val id: String? = null,
    val ticker: String? = null,
    val cik: String? = null,
    val exchange: String? = null,
    val companyUrl: String? = null,
    val companyName: String? = null,
    val price: Double? = null,
    val marketCap: Double? = null,
    val enterpriseValue: Double? = null,
    val perShareMetrics: PerShareMetrics = PerShareMetrics(),
    val latestMetrics: LatestMetrics = LatestMetrics(),
    val sales: Trend = Trend(),
    val earnings: Trend = Trend(),
    val tags: List<ZacksDerivedTag> = emptyList(),
)