package com.starburst.starburst.modelbuilder.common

import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.spreadsheet.Cell
import java.time.Instant

data class StockAnalysis(
    val _id: String,
    val lastUpdated: Instant = Instant.now(),
    /*
    value breakdown
     */
    val zeroGrowthPrice: Double,
    val impliedPriceFromGrowth: Double,
    val currentPrice: Double,

    val model: Model,
    val cells: List<Cell>,

    /*
    revenue waterfall by period
     */
    val businessWaterfall: Map<Int, Waterfall>,

    val shareOutstanding: Item,
    val profitPerShare: Item,

    /*
    target price
     */
    val targetPrice: Double,
    val discountRate: Double,

    val revenueCAGR: Double,
)
