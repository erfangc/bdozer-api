package com.bdozer.api.web.stockanalysis.models

import com.bdozer.api.models.dataclasses.Item
import com.bdozer.api.stockanalysis.models.Waterfall

/**
 * After a valuation model has been run,
 * here are some of the analytics that we can produce as an output
 */
data class DerivedStockAnalytics(
    /**
     * Simplified projection of future business based on the current scenario
     */
    val businessWaterfall: Map<Int, Waterfall>,
    val marketCap: Double? = null,
    val employees: Int? = null,
    val shareOutstanding: Item,
    val profitPerShare: Item,
    val targetPrice: Double,
    val finalPrice: Double? = null,
    val percentUpside: Double? = null,
    val discountRate: Double? = null,
    val revenueCAGR: Double? = null,
    val currentPrice: Double? = null,
    val irr: Double? = null,
)