package com.starburst.starburst.zacks.modelbuilder

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.fmtPct
import com.starburst.starburst.models.ReservedItemNames
import com.starburst.starburst.models.dataclasses.Commentary
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.zacks.dataclasses.ZacksFundamentalA

class IncomeStatementItemsBuilder {
    fun incomeStatementItems(fundamentalA: ZacksFundamentalA): List<Item> {
        val zacksRevenue = fundamentalA.tot_revnu ?: 0.0

        val revenue = Item(
            name = ReservedItemNames.Revenue,
            description = "Revenue",
            historicalValue = zacksRevenue,
            expression = "${fundamentalA.tot_revnu ?: 0.0}",
        )

        val zacksCogs = fundamentalA.cost_good_sold ?: 0.0
        val cogsPctOfRevenue = zacksCogs / zacksRevenue
        val cogs = Item(
            name = ReservedItemNames.CostOfGoodsSold,
            description = "Cost of Goods",
            historicalValue = zacksCogs,
            commentaries = Commentary(commentary = "Cost of goods sold has been ${cogsPctOfRevenue.fmtPct()}"),
            expression = "$cogsPctOfRevenue * ${ReservedItemNames.Revenue}",
        )
        val grossProfit = Item(
            name = ReservedItemNames.GrossProfit,
            description = "Gross Profit",
            historicalValue = fundamentalA.gross_profit ?: 0.0,
            expression = "${ReservedItemNames.Revenue} - ${ReservedItemNames.CostOfGoodsSold}",
        )
        val operatingExpense = Item(
            name = ReservedItemNames.OperatingExpense,
            description = "Operating Expense",
            historicalValue = fundamentalA.tot_oper_exp ?: 0.0,
            // TODO these need to be regressed against historical to determine variable vs. fixed component
            expression = "${fundamentalA.tot_oper_exp ?: 0.0}",
        )
        val operatingIncome = Item(
            name = ReservedItemNames.OperatingIncome,
            description = "Operating Income",
            historicalValue = fundamentalA.oper_income ?: 0.0,
            expression = "${ReservedItemNames.GrossProfit} - ${ReservedItemNames.OperatingExpense}",
        )
        val nonOperatingExpense = Item(
            name = ReservedItemNames.NonOperatingExpense,
            description = "Non-Operating Expense",
            historicalValue = fundamentalA.tot_non_oper_income_exp ?: 0.0,
            // TODO based on historical data determine whether to use average or mark them as one time
            expression = "${fundamentalA.tot_non_oper_income_exp ?: 0.0}",
        )

        val zacksPretaxIncome = fundamentalA.pre_tax_income ?: 0.0
        val pretaxIncome = Item(
            name = ReservedItemNames.PretaxIncome,
            description = "Pretax Income",
            historicalValue = zacksPretaxIncome,
            expression = "${ReservedItemNames.OperatingIncome} - ${ReservedItemNames.NonOperatingExpense}",
        )

        val zacksTaxExpense = zacksPretaxIncome - (fundamentalA.net_income_loss_share_holder ?: 0.0)
        val taxRate = 0.1.coerceAtLeast(zacksTaxExpense / zacksPretaxIncome)

        val taxExpense = Item(
            name = ReservedItemNames.TaxExpense,
            description = "Tax Expense",
            historicalValue = zacksTaxExpense,
            expression = "$taxRate * ${ReservedItemNames.PretaxIncome}",
            commentaries = Commentary("Pretax income is taxed at ${taxRate.fmtPct()}")
        )

        val netIncome = Item(
            name = ReservedItemNames.NetIncome,
            description = "Net Income",
            historicalValue = fundamentalA.net_income_loss_share_holder ?: 0.0,
            expression = "${ReservedItemNames.PretaxIncome} - ${ReservedItemNames.TaxExpense}"
        )

        return listOf(
            revenue,
            cogs,
            grossProfit,
            operatingExpense,
            operatingIncome,
            nonOperatingExpense,
            pretaxIncome,
            taxExpense,
            // TODO think about preferred dividend
            // TODO think about minority interest
            netIncome,
        )
    }

}