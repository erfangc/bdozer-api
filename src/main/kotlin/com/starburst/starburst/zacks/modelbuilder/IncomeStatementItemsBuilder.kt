package com.starburst.starburst.zacks.modelbuilder

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.fmtPct
import com.starburst.starburst.models.ReservedItemNames.CostOfGoodsSold
import com.starburst.starburst.models.ReservedItemNames.GrossProfit
import com.starburst.starburst.models.ReservedItemNames.NetIncome
import com.starburst.starburst.models.ReservedItemNames.NonOperatingExpense
import com.starburst.starburst.models.ReservedItemNames.OperatingExpense
import com.starburst.starburst.models.ReservedItemNames.OperatingIncome
import com.starburst.starburst.models.ReservedItemNames.PretaxIncome
import com.starburst.starburst.models.ReservedItemNames.Revenue
import com.starburst.starburst.models.ReservedItemNames.TaxExpense
import com.starburst.starburst.models.dataclasses.Commentary
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.ItemType
import com.starburst.starburst.zacks.dataclasses.KeyInputs
import com.starburst.starburst.zacks.dataclasses.ZacksFundamentalA

@ExperimentalStdlibApi
class IncomeStatementItemsBuilder(private val keyInputsProvider: KeyInputsProvider) {

    /**
     * Convert a [KeyInputs] into a [Revenue] [Item], this means
     * if custom key inputs exist on the [KeyInputs] under this ticker
     * it will be translated into a the correct formula and placed on the resulting
     * [Item]
     *
     * Or else, pre-saved revenue drivers such as [Discrete] might be used
     */
    private fun keyInputsToRevenue(fundamentalA: ZacksFundamentalA): Item {
        val ticker = fundamentalA.ticker
        val keyInputs = keyInputsProvider.getKeyInputs(ticker ?: error("..."))
        val totRevnu = fundamentalA.tot_revnu ?: 0.0

        val base = Item(
            name = Revenue,
            description = "Revenue",
            historicalValue = totRevnu,
        )

        return if (keyInputs.discrete != null) {
            base.copy(
                type = ItemType.Discrete,
                discrete = keyInputs.discrete
            )
        } else {
            // TODO parse the formula of keyInput(s) and replace them with the list of actual inputs
            base
        }
    }

    fun incomeStatementItems(fundamentalA: ZacksFundamentalA): List<Item> {

        val totRevnu = fundamentalA.tot_revnu ?: 0.0
        val costGoodSold = fundamentalA.cost_good_sold ?: 0.0
        val preTaxIncome = fundamentalA.pre_tax_income ?: 0.0

        val cogsPctOfRevenue = costGoodSold / totRevnu

        val netIncomeLossShareHolder = fundamentalA.net_income_loss_share_holder ?: 0.0
        val totNonOperIncomeExp = fundamentalA.tot_non_oper_income_exp ?: 0.0

        val taxesPaid = preTaxIncome - netIncomeLossShareHolder
        val taxRate = 0.1.coerceAtLeast(taxesPaid / preTaxIncome)

        val revenue = keyInputsToRevenue(fundamentalA)

        return listOf(
            revenue,
            Item(
                name = CostOfGoodsSold,
                description = "Cost of Goods",
                historicalValue = costGoodSold,
                commentaries = Commentary(commentary = "Cost of goods sold has been ${cogsPctOfRevenue.fmtPct()}"),
                expression = "$cogsPctOfRevenue * $Revenue",
            ),
            Item(
                name = GrossProfit,
                description = "Gross Profit",
                historicalValue = fundamentalA.gross_profit ?: 0.0,
                expression = "$Revenue - $CostOfGoodsSold",
            ),
            Item(
                name = OperatingExpense,
                description = "Operating Expense",
                historicalValue = fundamentalA.tot_oper_exp ?: 0.0,
                // TODO these need to be regressed against historical to determine variable vs. fixed component
                expression = "${fundamentalA.tot_oper_exp ?: 0.0}",
            ),
            Item(
                name = OperatingIncome,
                description = "Operating Income",
                historicalValue = fundamentalA.oper_income ?: 0.0,
                expression = "$GrossProfit-$OperatingExpense",
            ),
            Item(
                name = NonOperatingExpense,
                description = "Non-Operating Expense",
                historicalValue = fundamentalA.tot_non_oper_income_exp ?: 0.0,
                // TODO based on historical data determine whether to use average or mark them as one time
                expression = "$totNonOperIncomeExp",
            ),
            Item(
                name = PretaxIncome,
                description = "Pretax Income",
                historicalValue = preTaxIncome,
                expression = "$OperatingIncome-$NonOperatingExpense",
            ),
            Item(
                name = TaxExpense,
                description = "Tax Expense",
                historicalValue = taxesPaid,
                expression = "$taxRate*$PretaxIncome",
                commentaries = Commentary("Pretax income is taxed at ${taxRate.fmtPct()}")
            ),
            Item(
                name = NetIncome,
                description = "Net Income",
                historicalValue = fundamentalA.net_income_loss_share_holder ?: 0.0,
                expression = "$PretaxIncome-$TaxExpense"
            ),
        )
    }

}