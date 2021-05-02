package com.bdozer.stockanalysis.dataclasses

import com.bdozer.models.dataclasses.Model
import com.bdozer.spreadsheet.Cell
import java.time.Instant
import java.util.*

/**
 * This is the common output for all the Stock Analyzers
 */
data class StockAnalysis2(
    val _id: String = UUID.randomUUID().toString(),

    val name: String? = null,
    val description: String? = null,

    val cik: String? = null,
    val ticker: String? = null,

    val model: Model = Model(),
    val cells: List<Cell> = emptyList(),
    val derivedStockAnalytics: DerivedStockAnalytics? = null,

    val userId: String? = null,
    val tags: List<String> = emptyList(),
    val published: Boolean = false,
    val lastUpdated: Instant = Instant.now(),
)