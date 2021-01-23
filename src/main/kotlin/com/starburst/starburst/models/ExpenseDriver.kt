package com.starburst.starburst.models

import com.starburst.starburst.models.enums.ExpenseType

data class ExpenseDriver(
    // for example, "Salary"
    val name: String,
    val description: String? = null,
    // for example, R&D
    val incomeStatementItemName: String,
    val expenseType: ExpenseType,
    val percentOfRevenue: Double? = null,
    val initialValue: Double? = null,
    val fixedAmount: Double? = null,
    val growthYear: Int? = null
)
