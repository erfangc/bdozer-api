package com.starburst.starburst

import com.starburst.starburst.models.*
import com.starburst.starburst.models.enums.ExpenseType

/**
 * [DCFGenerator] service
 */
class DCFGenerator {

    fun generate(modelInput: ModelInput): ModelOutput {

        // calculate the revenue going forward
        val subscriptionDriver = modelInput.revenueDriver?.subscriptionDriver
        subscriptionDriver ?: error("subscriptionDriver cannot be null")

        // project a financial statement for each year that it takes this company
        // to reach its TAM
        val years = subscriptionDriver.yearsToReachTam ?: error("yearsToReachTam is required")

        (1..years).fold(emptyList<Period>(), operation = { acc, i ->

            val initialUnits = initialUnits(subscriptionDriver)
            val initialPerUnitRevenue = initialPerUnitRevenue(subscriptionDriver)
            val perYearGrowth = (subscriptionDriver.tam ?: error("tam is required")) / years
            val revenue = (perYearGrowth * i + initialUnits) * initialPerUnitRevenue

            // project the cash expenses for the period
            val expenseItems = modelInput.expenseDrivers.map { expenseDriver ->
                val value = when (expenseDriver.expenseType) {
                    ExpenseType.Variable ->
                        (expenseDriver.percentOfRevenue ?: 0.0).times(revenue)
                    ExpenseType.Fixed ->
                        expenseDriver.fixedAmount ?: 0.0
                }

                Item(
                    name = expenseDriver.incomeStatementItemName,
                    value = value,
                    type = Type.IncomeStatement,
                    contributors = listOf(
                        Contributor(
                            name = expenseDriver.name,
                            amount = value
                        )
                    )
                )

            }

            // project the depreciation & amortizations for the period

            // project the non-cash expenses for the period

            // compute other items from the previous balance-sheet and add CAPEX

            // compute cash flow statements

            val items = mutableListOf(
                Item(name = "Revenue", value = revenue, type = Type.IncomeStatement),
            )

            // create statements for this period
            val period = Period(
                items = items,
                year = i,
                quarter = 4
            )

            acc + period
        })

        TODO()
    }

    private fun initialPerUnitRevenue(subscriptionDriver: SubscriptionDriver): Double {
        return when {
            subscriptionDriver.perSubscriptionRevenue != null -> subscriptionDriver.perSubscriptionRevenue
            subscriptionDriver.initialRevenue != null && subscriptionDriver.initialUnits != null ->
                subscriptionDriver.initialRevenue / subscriptionDriver.initialUnits
            else -> error("this is not supposed to happen")
        }
    }

    private fun initialUnits(subscriptionDriver: SubscriptionDriver): Double {
        return when {
            subscriptionDriver.initialUnits != null -> subscriptionDriver.initialUnits.toDouble()
            subscriptionDriver.initialRevenue != null && subscriptionDriver.perSubscriptionRevenue != null ->
                subscriptionDriver.initialRevenue / subscriptionDriver.perSubscriptionRevenue
            else -> error("this is not supposed to happen")
        }
    }

}
