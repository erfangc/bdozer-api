package com.bdozer.api.stockanalysis.models

import com.bdozer.api.models.dataclasses.Model
import com.bdozer.api.models.dataclasses.spreadsheet.Cell
import com.bdozer.api.web.stockanalysis.models.DerivedStockAnalytics
import com.bdozer.api.web.stockanalysis.models.ZacksDerivedAnalytics
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
    val industry: String? = null,
    val sector: String? = null,
    val url: String? = null,
    val similar: List<String>? = null,
    val ceo: String? = null,
    val country: String? = null,

    val zacksDerivedAnalytics: ZacksDerivedAnalytics = ZacksDerivedAnalytics(),

    val userId: String? = null,
    val tags: List<String> = emptyList(),
    val published: Boolean = false,
    val lastUpdated: Instant = Instant.now(),
)
