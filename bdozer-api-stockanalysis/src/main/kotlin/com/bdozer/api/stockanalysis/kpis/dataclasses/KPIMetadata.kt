package com.bdozer.api.stockanalysis.kpis.dataclasses

data class KPIMetadata(
    val itemName: String,
    val description: String? = null,
    val format: Format,
    val collapse: Boolean? = null,
)

