package com.bdozer.stockanalyzer.dataclasses

import com.bdozer.models.dataclasses.Item
import com.bdozer.models.dataclasses.Model
import com.bdozer.spreadsheet.Cell
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * This is the common output for all the Stock Analyzers
 */
data class StockAnalysis(
    val _id: String = UUID.randomUUID().toString(),

    val cik: String,
    val adsh: String? = null,
    val userId: String? = null,
    val ticker: String? = null,
    val lastUpdated: Instant = Instant.now(),

    /*
    value breakdown
     */
    val zeroGrowthPrice: Double,

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
    val currentPrice: Double = 0.0,

    /*
    crucial Item / concept names
     */
    val totalRevenueConceptName: String,
    val epsConceptName: String,
    val netIncomeConceptName: String,
    val ebitConceptName: String,
    val operatingCostConceptName: String,
    val sharesOutstandingConceptName: String,
)
