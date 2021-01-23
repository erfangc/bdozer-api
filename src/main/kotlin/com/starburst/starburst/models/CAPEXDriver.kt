package com.starburst.starburst.models

import com.starburst.starburst.models.enums.DepreciationMethod
import com.starburst.starburst.models.enums.ExpenseType

data class CAPEXDriver(
    // for example, "Salary"
    val name: String,
    val description: String? = null,
    // for example, R&D
    val incomeStatementItemName: String,
    val depreciationMethod: DepreciationMethod = DepreciationMethod.StraightLine,
    val usefulLife: Int? = null,
    val expenseType: ExpenseType,
    val percentOfRevenue: Double? = null,
    val initialValue: Double? = null,
    val initialShares: Int? = null,
    val growthRate: Double? = null,
    val growthYear: Int? = null
)
