package com.starburst.starburst.models.builders

import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.ItemType
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.models.Utility
import com.starburst.starburst.models.dataclasses.PercentOfRevenue
import com.starburst.starburst.models.dataclasses.SubscriptionRevenue

object SkeletonModel {
    val dropbox = Model(
        periods = 5,
        currentPrice = 23.02,
        beta = 0.88,
        sharesOutstanding = 414.3,
        terminalFcfMultiple = 19.2,
        symbol = "DBX",
        name = "Sample Model",
        incomeStatementItems = listOf(
            Item(
                name = "Subscriptions",
                description = "Cloud Storage Subs",
                subscriptionRevenue = SubscriptionRevenue(
                    totalSubscriptionAtTerminalYear = 22.0,
                    initialSubscriptions = 15.48,
                    averageRevenuePerSubscription = 123.6,
                ),
                type = ItemType.SubscriptionRevenue,
                historicalValue = 1913.9
            ),
            Item(
                name = Utility.Revenue,
                description = "Revenue"
            ),
            Item(
                name = "CashVariableExpenses",
                description = "Cash Variable Expenses",
                type = ItemType.PercentOfRevenue,
                historicalValue = 397.5,
                percentOfRevenue = PercentOfRevenue(
                    percentOfRevenue = 0.2076911019
                )
            ),
            Item(
                name = "SBCVariableExpenses",
                description = "Stock Based Compensation",
                type = ItemType.PercentOfRevenue,
                historicalValue = 17.1,
                percentOfRevenue = PercentOfRevenue(
                    percentOfRevenue = 0.008934636083
                ),
                stockBasedCompensation = true
            ),
            Item(
                name = Utility.CostOfGoodsSold,
                description = "Cost of Goods Sold"
            ),
            Item(
                name = Utility.GrossProfit,
                description = "Gross Profit"
            ),
            Item(
                name = "RD",
                description = "Research & Development",
                expression = "min(800,0.28914*Revenue)",
                historicalValue = 553.4
            ),
            Item(
                name = "SBC_RD",
                description = "Stock Based Compensation",
                expression = "min(800,0.0909*Revenue)",
                historicalValue = 174.1,
                stockBasedCompensation = true
            ),
            Item(
                name = "Selling",
                description = "Selling",
                expression = "422.8",
                historicalValue = 422.8
            ),
            Item(
                name = "GA",
                description = "General & Administrative",
                expression = "227.8",
                historicalValue = 227.8
            ),
            Item(
                name = "Impairment",
                description = "Impairment",
                expression = "0.0",
                historicalValue = 398.2,
                nonCashExpense = true
            ),
            Item(
                name = Utility.OperatingExpense,
                description = "Operating Expense"
            ),
            Item(
                name = Utility.OperatingIncome,
                description = "Operating Income"
            ),
            Item(
                name = "Other_Income",
                description = "Other Income",
                expression = "0.0",
                historicalValue = -26.8
            ),
            Item(
                name = Utility.NonOperatingExpense,
                description = "NonOperating Expense"
            ),
            Item(
                name = Utility.InterestExpense,
                expression = "0.0",
                description = "Interest Expense"
            ),
            Item(
                name = Utility.TaxExpense,
                description = "Tax Expense",
                historicalValue = 6.1
            ),
            Item(
                name = Utility.NetIncome,
                description = "Net Income"
            )
        ),
        balanceSheetItems = listOf(
            Item(
                name = Utility.CurrentAsset,
                historicalValue = 0.0
            ),
            Item(
                name = Utility.LongTermAsset,
                historicalValue = 0.0
            ),
            Item(
                name = Utility.TotalAsset,
                historicalValue = 0.0,
                expression = "${Utility.CurrentAsset}+${Utility.LongTermAsset}"
            ),
            Item(
                name = Utility.CurrentLiability,
                historicalValue = 0.0
            ),
            Item(
                name = Utility.LongTermLiability,
                historicalValue = 0.0
            ),
            Item(
                name = Utility.TotalLiability,
                historicalValue = 0.0,
                expression = "${Utility.CurrentLiability}+${Utility.LongTermLiability}"
            ),
            Item(
                name = Utility.ShareholdersEquity,
                historicalValue = 0.0,
                expression = "${Utility.TotalAsset}-${Utility.TotalLiability}"
            )
        )
    )
}
