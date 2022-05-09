package co.bdozer.libraries.master.models

data class Trend(
    val isIncreasing: Boolean,
    val isErratic: Boolean,
    val thisQuarter: Double?,
    val oneQuarterAgo: Double?,
    val twoQuartersAgo: Double?,
    val threeQuartersAgo: Double?,
    val fourQuartersAgo: Double?,
    val thisQuarterPctChange: Double?,
    val oneQuarterAgoPctChange: Double?,
    val twoQuartersAgoPctChange: Double?,
    val threeQuartersAgoPctChange: Double?,
    val fourQuartersAgoPctChange: Double?,
)