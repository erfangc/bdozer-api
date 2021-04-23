package com.bdozer.models.dataclasses

import com.bdozer.models.Utility

data class Model(

    val ticker: String? = null,
    val cik: String? = null,

    /**
     * The SEC filing adsh from
     * which the automated model generate from
     */
    val adsh: String? = null,

    val name: String? = null,

    /**
     * Manual overrides for items
     */
    val itemOverrides: List<Item> = emptyList(),
    /**
     * Items that should be removed from calculation
     * altogether
     */
    val suppressedItems: List<String> = emptyList(),

    /**
     * Crucial Item / concept names
     */
    val totalRevenueConceptName: String? = null,
    val epsConceptName: String? = null,
    val netIncomeConceptName: String? = null,
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
) {

    fun override(): Model {
        val suppressedItems = suppressedItems.toSet()
        val overrideLookup = itemOverrides.associateBy { it.name }
        fun overrideItems(items: List<Item>): List<Item> {
            return items
                .filter { item -> !suppressedItems.contains(item.name) }
                .map { item -> overrideLookup[item.name] ?: item }
        }
        return copy(
            incomeStatementItems = overrideItems(incomeStatementItems),
            balanceSheetItems = overrideItems(balanceSheetItems),
            cashFlowStatementItems = overrideItems(cashFlowStatementItems),
        )
    }

    fun generateOtherItems(): List<Item> {
        val epsConceptName = epsConceptName
        val periods = periods
        val discountRate = (equityRiskPremium * beta) + riskFreeRate
        val terminalPeMultiple = 1.0 / (discountRate - terminalGrowthRate)

        return listOf(
            Item(
                name = Utility.DiscountFactor,
                formula = "1 / (1.0 + $discountRate)^period",
            ),
            Item(
                name = Utility.TerminalValuePerShare,
                formula = "if(period=$periods,$epsConceptName * $terminalPeMultiple,0.0)",
            ),
            Item(
                name = Utility.PresentValueOfTerminalValuePerShare,
                formula = "${Utility.DiscountFactor} * ${Utility.TerminalValuePerShare}",
            ),
            Item(
                name = Utility.PresentValueOfEarningsPerShare,
                formula = "${Utility.DiscountFactor} * $epsConceptName",
            ),
            Item(
                name = Utility.PresentValuePerShare,
                formula = "${Utility.PresentValueOfEarningsPerShare} + ${Utility.PresentValueOfTerminalValuePerShare}",
            )
        )
    }

    fun allItems(): List<Item> {
        return (incomeStatementItems + balanceSheetItems + cashFlowStatementItems + otherItems)
    }

    fun item(itemName: String?): Item? {
        return allItems().find { it.name == itemName }
    }

}