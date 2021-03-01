package com.starburst.starburst.models

import org.javers.core.metamodel.annotation.Id
import java.time.Instant
import java.util.*

data class Model(
    @Id
    val _id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Model",
    val symbol: String? = null,
    val cik: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),

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
    val currentPrice: Double = 1.0,
    val beta: Double = 1.0,
    val sharesOutstanding: Double = 1.0,
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
    val periods: Int = 5,

    val updatedAt: Instant = Instant.now(),
    val updatedBy: String? = null
)
