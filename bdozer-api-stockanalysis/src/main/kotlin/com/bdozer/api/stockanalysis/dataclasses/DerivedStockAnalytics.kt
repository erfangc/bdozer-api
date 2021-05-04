package com.bdozer.api.stockanalysis.dataclasses

import com.bdozer.api.models.dataclasses.Item

/**
 * After a valuation model has been run,
 * here are some of the analytics that we can produce as an output
 */
data class DerivedStockAnalytics(
    /**
     * value breakdown
     */
    val zeroGrowthPrice: Double,

    /**
     * revenue waterfall by period
     */
    val businessWaterfall: Map<Int, Waterfall>,
    val shareOutstanding: Item,
    val profitPerShare: Item,

    /**
     * target price
     */
    val targetPrice: Double,
    val discountRate: Double,
    val revenueCAGR: Double = 0.0,
    val currentPrice: Double,
    val irr: Double? = null,
)