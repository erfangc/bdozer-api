package com.starburst.starburst.models

data class SegmentAssumptions(
    val segment: String,
    val description: String? = null,
    val revenueDriver: RevenueDriver,
    val expenseDrivers: List<ExpenseDriver>,
    val stockCompensationDrivers: List<StockCompensationDriver>,
    val capexDrivers: List<CAPEXDriver>
)
