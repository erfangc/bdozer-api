package com.starburst.starburst.models

data class Model(

    val incomeStatementItems: List<Item> = emptyList(),
    val balanceSheetItems: List<Item> = emptyList(),
    val otherItems: List<Item> = emptyList(),

    /*
    assumptions
     */
    val currentPrice: Double = 1.0,
    val beta: Double = 1.0,
    val sharesOutstanding: Double? = null,
    val dilutedSharesOutstanding: Double? = null,
    val corporateTaxRate: Double = 0.1,
    val costOfDebt: Double = 0.05,

    /*
    projection period
     */
    val periods: Int = 5

)
