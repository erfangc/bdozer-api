package com.starburst.starburst.stockanalyzer.staging.dataclasses

import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.spreadsheet.Cell
import java.time.Instant
import java.util.*

/**
 * This is the common output for all the Stock Analyzers
 */
data class StockAnalysis2(
    val _id: String = UUID.randomUUID().toString(),

    val name: String = "Untitled Stock Analysis",
    val description: String? = null,

    val cik: String? = null,
    val ticker: String? = null,

    val model: Model = Model(),
    val cells: List<Cell> = emptyList(),
    val derivedStockAnalytics: DerivedStockAnalytics? = null,

    val userId: String? = null,
    val tags: List<String> = emptyList(),
    val lastUpdated: Instant = Instant.now(),
)