package com.bdozer.models.dataclasses

import com.bdozer.models.Utility.DiscountFactor
import com.bdozer.models.Utility.PresentValueOfEarningsPerShare
import com.bdozer.models.Utility.PresentValueOfTerminalValuePerShare
import com.bdozer.models.Utility.PresentValuePerShare
import com.bdozer.models.Utility.TerminalValuePerShare

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

        val periods = periods
        val epsConceptName = epsConceptName
        val discountRate = (equityRiskPremium * beta) + riskFreeRate
        val terminalPeMultiple = 1.0 / (discountRate - terminalGrowthRate)

        return listOf(
            Item(
                name = DiscountFactor,
                formula = "1 / (1.0 + $discountRate)^period",
            ),
            Item(
                name = TerminalValuePerShare,
                formula = "if(period=$periods,$epsConceptName * $terminalPeMultiple,0.0)",
            ),
            Item(
                name = PresentValueOfTerminalValuePerShare,
                formula = "$DiscountFactor * $TerminalValuePerShare",
            ),
            Item(
                name = PresentValueOfEarningsPerShare,
                formula = "$DiscountFactor * $epsConceptName",
            ),
            Item(
                name = PresentValuePerShare,
                formula = "$PresentValueOfEarningsPerShare + $PresentValueOfTerminalValuePerShare",
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