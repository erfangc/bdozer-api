package com.starburst.starburst.models.translator.subtypes.dataclasses

data class UnitSalesRevenue(
    val steadyStateUnitsSold: Double = 0.0,
    val averageSellingPrice: Double = 0.0,
    val initialUnitsSold: Double = 0.0
)
