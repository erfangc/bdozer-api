package com.starburst.starburst

import com.starburst.starburst.computers.ReservedItemNames
import com.starburst.starburst.models.Driver
import com.starburst.starburst.models.DriverType
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.translator.subtypes.SaaSRevenue
import com.starburst.starburst.models.translator.subtypes.VariableCost

object Provier {
    fun fictitiousSaaSCompany() = Model(
        sharesOutstanding = 1_000_000.0,
        incomeStatementItems = listOf(
            Item(
                name = "Cloud_Hosting_Revenue",
                drivers = listOf(
                    Driver(
                        name = "Subscription",
                        type = DriverType.SaaSRevenue,
                        saaSRevenue = SaaSRevenue(
                            totalSubscriptionAtTerminalYear = 100_000,
                            initialSubscriptions = 1_000,
                            averageRevenuePerSubscription = 120.0
                        )
                    )
                )
            ),
            Item(
                name = ReservedItemNames.Revenue
            ),
            Item(
                name = "Cloud_Hosting_COGS",
                drivers = listOf(
                    Driver(
                        name = "Equipment",
                        type = DriverType.VariableCost,
                        variableCost = VariableCost(
                            percentOfRevenue = 0.2
                        )
                    )
                )
            ),
            Item(
                name = ReservedItemNames.CostOfGoodsSold,
            ),

            Item(
                name = ReservedItemNames.GrossProfit
            ),
            Item(
                name = "ResearchAndDevelopment",
                expression = "Revenue * 0.14"
            ),
            Item(
                name = "SGA",
                expression = "100000"
            ),
            Item(
                name = ReservedItemNames.OperatingExpense
            ),
            Item(
                name = ReservedItemNames.OperatingIncome
            ),
            Item(
                name = ReservedItemNames.NonOperatingExpense
            ),
            Item(
                name = ReservedItemNames.InterestExpense
            ),
            Item(
                name = ReservedItemNames.TaxExpense
            ),
            Item(
                name = ReservedItemNames.NetIncome
            )
        ),
        balanceSheetItems = listOf(
            Item(
                name = ReservedItemNames.CurrentAsset,
                historicalValue = 1_000_000.0
            ),
            Item(
                name = ReservedItemNames.LongTermAsset,
                historicalValue = 12_000_000.0
            ),
            Item(
                name = ReservedItemNames.TotalAsset,
                historicalValue = 13_000_000.0,
                expression = "${ReservedItemNames.CurrentAsset}+${ReservedItemNames.LongTermAsset}"
            ),
            Item(
                name = ReservedItemNames.CurrentLiability,
                historicalValue = 500_000.0
            ),
            Item(
                name = ReservedItemNames.LongTermLiability,
                historicalValue = 3_500_000.0
            ),
            Item(
                name = ReservedItemNames.TotalLiability,
                historicalValue = 4_000_000.0,
                expression = "${ReservedItemNames.CurrentLiability}+${ReservedItemNames.LongTermLiability}"
            ),
            Item(
                name = ReservedItemNames.ShareholdersEquity,
                historicalValue = 9_000_000.0,
                expression = "${ReservedItemNames.TotalAsset}-${ReservedItemNames.TotalLiability}"
            )
        ),
        terminalFcfMultiple = 15.0
    )

}
