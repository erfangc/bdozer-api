package com.bdozer.api.stockanalysis.kpis.dataclasses

import com.bdozer.api.models.dataclasses.Item
import com.bdozer.api.models.dataclasses.spreadsheet.Cell

/**
 * [CompanyKPIs] represents a comprehensive model for company revenue
 * projection via KPIs
 *
 * KPIs are represented as ordinary [Item] with enhanced metadata via [KPIMetadata] lookup
 * by `itemName`. The [Item] can be operated on similar to [Model] objects and turned into evaluated
 * [Cell] instances that represent actual projections for the KPIs and ultimately revenue across time
 */
data class CompanyKPIs(
    val _id: String,
    val cik: String,
    val kpis: List<KPIMetadata>,
    val revenueItemName: String,
    val items: List<Item> = emptyList(),
    val cells: List<Cell> = emptyList(),
    val projectionPeriods: Int = 5,
)
