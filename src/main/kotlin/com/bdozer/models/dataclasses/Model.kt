package com.bdozer.models.dataclasses

data class Model(
    /**
     * The SEC filing adsh from
     * which the automated model generate from
     */
    val adsh: String? = null,

    /**
     * Manual overrides for items
     */
    val itemOverrides: List<Item> = emptyList(),

    /**
     * Crucial Item / concept names
     */
    val totalRevenueConceptName: String? = null,
    val epsConceptName: String? = null,
    val netIncomeConceptName: String? = null,
    val ebitConceptName: String? = null,
    val operatingCostConceptName: String? = null,
    val sharesOutstandingConceptName: String? = null,

    /**
     * The main statements
     */
    val incomeStatementItems: List<Item> = emptyList(),
    val balanceSheetItems: List<Item> = emptyList(),
    val cashFlowStatementItems: List<Item> = emptyList(),
    val otherItems: List<Item> = emptyList(),

    /**
     * Assumptions
     */
    val beta: Double = 1.0,
    val riskFreeRate: Double = 0.005,
    val equityRiskPremium: Double = 0.075,
    val terminalGrowthRate: Double = 0.02,

    /**
     * Projection period
     */
    val periods: Int = 5,

    /**
     * Excel metadata
     */
    val excelColumnOffset: Int = 1,
    val excelRowOffset: Int = 1,
)