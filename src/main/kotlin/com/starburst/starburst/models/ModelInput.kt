package com.starburst.starburst.models

data class ModelInput(
    val segmentAssumptions: List<SegmentAssumptions> = emptyList(),
    val revenueDriver: RevenueDriver? = null,
    val expenseDrivers: List<ExpenseDriver> = emptyList(),
    val stockCompensationDrivers: List<StockCompensationDriver> = emptyList(),
    val capexDrivers: List<CAPEXDriver> = emptyList(),
    val taxRate: Double? = null,
    // the most recent piece of historical data
    val lastHistorical: Period? = null
)
