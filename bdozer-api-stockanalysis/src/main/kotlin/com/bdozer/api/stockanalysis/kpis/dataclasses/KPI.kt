package com.bdozer.api.stockanalysis.kpis.dataclasses

import java.time.LocalDate

data class KPI(
    val itemName: String,
    val description: String? = null,
    val format: Format,
    val value: Double,
    val date: LocalDate,
    val collapse: Boolean? = null,
)

