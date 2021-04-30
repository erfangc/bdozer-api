package com.bdozer.models.dataclasses

enum class ItemType {
    Discrete,
    CompoundedGrowth,
    SumOfOtherItems,
    Custom,
    ManualProjections,
    PercentOfRevenue,
    PercentOfAnotherItem,
    FixedCost,
}

data class ManualProjections(
    val manualProjections: List<ManualProjection>,
)

data class ManualProjection(
    val period: Int,
    val value: Double,
)