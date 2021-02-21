package com.starburst.starburst

import com.starburst.starburst.computers.ReservedItemNames
import com.starburst.starburst.models.*
import com.starburst.starburst.models.builders.ModelBuilder
import com.starburst.starburst.models.translator.subtypes.SaaSRevenue
import com.starburst.starburst.models.translator.subtypes.VariableCost
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("models")
class ModelsController(private val modelBuilder: ModelBuilder) {

    @GetMapping("default")
    fun default(): Model {
        return modelBuilder.reformulateModel(defaultModel())
    }

    companion object {
        fun defaultModel() = Model(
            sharesOutstanding = 1_000_000.0,
            incomeStatementItems = listOf(
                Item(
                    name = "Cloud_Hosting_Revenue",
                    description = "Cloud Hosting Revenue",
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
                    ),
                    historicalValue = 120_000.0
                ),
                Item(
                    name = ReservedItemNames.Revenue
                ),
                Item(
                    name = "Cloud_Hosting_COGS",
                    description = "Cloud Hosting COGS",
                    drivers = listOf(
                        Driver(
                            name = "Equipment",
                            type = DriverType.VariableCost,
                            variableCost = VariableCost(
                                percentOfRevenue = 0.2
                            )
                        )
                    ),
                    historicalValue = 24_000.0
                ),
                Item(
                    name = ReservedItemNames.CostOfGoodsSold,
                    description = "Cost of Goods Sold",
                ),

                Item(
                    name = ReservedItemNames.GrossProfit,
                    description = "Gross Profit",
                ),
                Item(
                    name = "Research_and_Development",
                    description  = "Research and Development",
                    expression = "Revenue * 0.14",
                    historicalValue = 16_800.0
                ),
                Item(
                    name = "SGA",
                    description  = "Selling General & Administrative",
                    expression = "100000",
                    historicalValue = 100000.0
                ),
                Item(
                    name = ReservedItemNames.OperatingExpense,
                    description = "Operating Expense"
                ),
                Item(
                    name = ReservedItemNames.OperatingIncome,
                    description = "Operating Income"
                ),
                Item(
                    name = ReservedItemNames.NonOperatingExpense,
                    description = "Non-Operating Expense"
                ),
                Item(
                    name = ReservedItemNames.InterestExpense,
                    description = "Interest Expense"
                ),
                Item(
                    name = ReservedItemNames.TaxExpense,
                    description = "Tax Expense"
                ),
                Item(
                    name = ReservedItemNames.NetIncome,
                    description = "Net Income"
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
}
