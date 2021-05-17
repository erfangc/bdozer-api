package com.bdozer.api.stockanalysis.kpis.dataclasses

import com.bdozer.api.models.dataclasses.Item

data class CompanyKPIs(
    val _id: String,
    val cik: String,
    val kpis: List<KPI>,
    val revenueItemName: String,
    val items: List<Item>,
)
