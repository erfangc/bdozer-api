package com.bdozer.api.stockanalysis.models

data class Trend(
    val isIncreasing: Boolean = false,
    val isErratic: Boolean = false,
    val thisQuarter: Double? = null,
    val oneQuarterAgo: Double? = null,
    val twoQuartersAgo: Double? = null,
    val threeQuartersAgo: Double? = null,
    val fourQuartersAgo: Double? = null,
    val thisQuarterPctChange: Double? = null,
    val oneQuarterAgoPctChange: Double? = null,
    val twoQuartersAgoPctChange: Double? = null,
    val threeQuartersAgoPctChange: Double? = null,
    val fourQuartersAgoPctChange: Double? = null,
)