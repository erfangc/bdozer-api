package com.starburst.starburst.models

import com.starburst.starburst.models.enums.ExpenseType

data class StockCompensationDriver(
    // for example, "Salary"
    val name: String,
    val description: String? = null,
    // for example, R&D
    val incomeStatementItemName: String,
    val expenseType: ExpenseType = ExpenseType.Variable,
    val percentOfRevenue: Double? = null,
    val initialValue: Double? = null,
    val initialShares: Int? = null,
    val growthRate: Double? = null,
    val growthYear: Int? = null
)

