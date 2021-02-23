package com.starburst.starburst.models.builders

import com.starburst.starburst.models.ReservedItemNames
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model

object SkeletonModel {
    val skeletonModel = Model(
        periods = 5,
        incomeStatementItems = listOf(
            Item(
                name = ReservedItemNames.Revenue,
                expression = "0.0",
                description = "Revenue"
            ),
            Item(
                name = ReservedItemNames.CostOfGoodsSold,
                expression = "0.0",
                description = "Cost of Goods Sold"
            ),
            Item(
                name = ReservedItemNames.GrossProfit,
                expression = "${ReservedItemNames.Revenue} - ${ReservedItemNames.CostOfGoodsSold}",
                description = "Gross Profit"
            ),
            Item(
                name = ReservedItemNames.OperatingExpense,
                expression = "0.0",
                description = "Operating Expense"
            ),
            Item(
                name = ReservedItemNames.OperatingIncome,
                expression = "${ReservedItemNames.GrossProfit} - ${ReservedItemNames.OperatingExpense}",
                description = "Operating Income"
            ),
            Item(
                name = ReservedItemNames.NonOperatingExpense,
                expression = "0.0",
                description = "NonOperating Expense"
            ),
            Item(
                name = ReservedItemNames.InterestExpense,
                expression = "0.0",
                description = "Interest Expense"
            ),
            Item(
                name = ReservedItemNames.TaxExpense,
                expression = "0.0",
                description = "Tax Expense"
            ),
            Item(
                name = ReservedItemNames.NetIncome,
                expression = "${ReservedItemNames.OperatingIncome} - ${ReservedItemNames.NonOperatingExpense} - ${ReservedItemNames.InterestExpense} - ${ReservedItemNames.TaxExpense}",
                description = "Net Income"
            )
        ),
        balanceSheetItems = listOf(
            Item(
                name = ReservedItemNames.CurrentAsset,
                historicalValue = 0.0
            ),
            Item(
                name = ReservedItemNames.LongTermAsset,
                historicalValue = 0.0
            ),
            Item(
                name = ReservedItemNames.TotalAsset,
                historicalValue = 0.0,
                expression = "${ReservedItemNames.CurrentAsset}+${ReservedItemNames.LongTermAsset}"
            ),
            Item(
                name = ReservedItemNames.CurrentLiability,
                historicalValue = 0.0
            ),
            Item(
                name = ReservedItemNames.LongTermLiability,
                historicalValue = 0.0
            ),
            Item(
                name = ReservedItemNames.TotalLiability,
                historicalValue = 0.0,
                expression = "${ReservedItemNames.CurrentLiability}+${ReservedItemNames.LongTermLiability}"
            ),
            Item(
                name = ReservedItemNames.ShareholdersEquity,
                historicalValue = 0.0,
                expression = "${ReservedItemNames.TotalAsset}-${ReservedItemNames.TotalLiability}"
            )
        )
    )
}
