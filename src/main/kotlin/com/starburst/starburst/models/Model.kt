package com.starburst.starburst.models

data class Model(

    val incomeStatementItems: List<Item> = emptyList(),
    val balanceSheetItems: List<Item> = emptyList(),
    val otherItems: List<Item> = emptyList(),

    /**
     * Assumptions
     */
    val currentPrice: Double = 1.0,
    val beta: Double = 1.0,
    val sharesOutstanding: Double? = null,
    val dilutedSharesOutstanding: Double? = null,
    val corporateTaxRate: Double = 0.1,
    val costOfDebt: Double = 0.04,
    val riskFreeRate: Double = 0.005,
    val equityRiskPremium: Double = 0.07,
    val terminalFcfMultiple: Double = 10.0,
    val terminalFcfGrowthRate: Double = 0.035,

    /**
     * Projection period
     */
    val periods: Int = 5

)
