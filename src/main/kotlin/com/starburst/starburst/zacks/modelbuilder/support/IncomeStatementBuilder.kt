package com.starburst.starburst.zacks.modelbuilder.support

import com.starburst.starburst.DoubleExtensions.fmtPct
import com.starburst.starburst.DoubleExtensions.fmtRound
import com.starburst.starburst.DoubleExtensions.orZero
import com.starburst.starburst.edgar.factbase.support.FactComponentFinder
import com.starburst.starburst.models.Utility.CostOfGoodsSold
import com.starburst.starburst.models.Utility.GrossProfit
import com.starburst.starburst.models.Utility.NetIncome
import com.starburst.starburst.models.Utility.NonOperatingExpense
import com.starburst.starburst.models.Utility.OperatingExpense
import com.starburst.starburst.models.Utility.OperatingIncome
import com.starburst.starburst.models.Utility.PretaxIncome
import com.starburst.starburst.models.Utility.Revenue
import com.starburst.starburst.models.Utility.TaxExpense
import com.starburst.starburst.models.dataclasses.Commentary
import com.starburst.starburst.models.dataclasses.Discrete
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.zacks.dataclasses.Context
import com.starburst.starburst.zacks.dataclasses.IncomeStatement
import com.starburst.starburst.zacks.fa.ZacksFundamentalA
import com.starburst.starburst.zacks.dataclasses.KeyInputs
import org.springframework.stereotype.Service

@Service
class IncomeStatementBuilder(
    private val keyInputsProvider: KeyInputsProvider,
    private val factComponentFinder: FactComponentFinder,
) {

    private fun findFwrdCogsPCt(fundamentalAs: List<ZacksFundamentalA>): Double? {
        if (fundamentalAs.isEmpty()) {
            return null
        }
        val annualCogsPct = fundamentalAs.map { row ->
            row.cost_good_sold.orZero() / row.tot_revnu.orZero()
        }
        val latestCogsPct = annualCogsPct.first()

        return if (latestCogsPct > 1.0) {
            // go back to last time where it was not > 1.0
            annualCogsPct.subList(1, annualCogsPct.size).find { it < 1.0 }
        } else {
            latestCogsPct
        }
    }

    /**
     * Model Cost of Goods sold by looking at historical
     * relationship between COGS and Revenue
     */
    private fun costOfGoods(ctx: Context): Item {
        val latest = ctx.latestAnnual()
        val ticker = latest.ticker!!
        val fundamentalAs = ctx.zacksFundamentalA

        val annual = fundamentalAs
            .filter { it.per_type == "Q" }
            .sortedByDescending { it.per_end_date }
        val quarterly = fundamentalAs
            .filter { it.per_type == "A" }
            .sortedByDescending { it.per_end_date }

        /*
        Basic logic:

        - If COGS / Revenue is > 1 - go back to the latest time it's been < 1 or else make it == Revenue
        - If COGS / Revenue has been steady then use the latest
        - If COGS / Revenue has been increasing or increasing but < 1, then roll forward the increase/decrease by 1 period

        Do the above calculation first annual if the data is not available then fallback to quarterly
         */

        val fwrdCogsPct = findFwrdCogsPCt(annual)
            ?: findFwrdCogsPCt(quarterly)
            ?: error("unable to determine forward going COGS pct of revenue for $ticker")

        val cogsHist = quarterly.sortedBy { it.per_end_date }.map { it.cost_good_sold }
        val earliestCogs = cogsHist.first().orZero()
        val latestCogs = cogsHist.last().orZero()
        val verb = if (latestCogs > earliestCogs) {
            "up"
        } else {
            "down"
        }

        return Item(
            name = CostOfGoodsSold,
            description = "Cost of Goods",
            historicalValue = latest.cost_good_sold.orZero(),
            commentaries = Commentary(
                commentary = """
                |Cost of goods sold has historically been ${fwrdCogsPct.fmtPct()} of revenue. 
                |It's been trending $verb. 
                |Out of conservatism, We will assume ${fwrdCogsPct.fmtPct()} going forward
                |""".trimMargin()
            ),
            expression = "$fwrdCogsPct * $Revenue",
        )
    }

    fun incomeStatementItems(ctx: Context, keyInputs: KeyInputs? = null): IncomeStatement {
        val latest = ctx.latestAnnual()

        /*
        Extract and prepare some basic inputs
         */
        val preTaxIncome = latest.pre_tax_income.orZero()
        val netIncomeLossShareHolder = latest.net_income_loss_share_holder.orZero()
        val totNonOperIncomeExp = latest.tot_non_oper_income_exp.orZero()

        /*
        Compute some basic metrics that will be used
        to drive balance sheet items
         */
        val costOfGoods = costOfGoods(ctx)
        val taxesPaid = preTaxIncome - netIncomeLossShareHolder
        val taxRate = (taxesPaid / preTaxIncome)
            .coerceAtLeast(0.08)
            .coerceAtMost(0.15)

        val revenue = keyInputsToRevenue(ctx, keyInputs)
        val operatingExpenses = operatingExpenses(ctx)

        /*
        Collate the items in the order they should appear
        with subtotaling relationship defined
         */
        val items = listOf(
            revenue,
            costOfGoods,
            Item(
                name = GrossProfit,
                description = "Gross Profit",
                historicalValue = latest.gross_profit.orZero(),
                expression = "$Revenue - $CostOfGoodsSold",
            ),
            operatingExpenses,
            Item(
                name = OperatingIncome,
                description = "Operating Income",
                historicalValue = latest.oper_income.orZero(),
                expression = "$GrossProfit-$OperatingExpense",
            ),
            Item(
                name = NonOperatingExpense,
                description = "Non-Operating Expense",
                historicalValue = latest.tot_non_oper_income_exp.orZero(),
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
                historicalValue = latest.net_income_loss_share_holder.orZero(),
                expression = "$PretaxIncome-$TaxExpense"
            ),
        )

        return IncomeStatement(
            items = items
        )
    }

    /**
     * Convert a [KeyInputs] into a [Revenue] [Item], this means
     * if custom key inputs exist on the [KeyInputs] under this ticker
     * it will be translated into a the correct formula and placed on the resulting
     * [Item]
     *
     * Or else, pre-saved revenue drivers such as [Discrete] might be used
     */
    private fun keyInputsToRevenue(
        ctx: Context,
        keyInputs: KeyInputs? = null
    ): Item {
        val latest = ctx.latestAnnual()
        val ticker = latest.ticker ?: error("...")
        val keyInputs = keyInputs ?: keyInputsProvider.getKeyInputs(ticker)
        val totRevnu = latest.tot_revnu.orZero()

        val salesEstimateToRevenueConverter = SalesEstimateToRevenueConverter(ctx)

        return if (keyInputs == null) {
            salesEstimateToRevenueConverter.convert(totRevnu)
        } else {
            // TODO resolve the formula Key inputs
            val formula = keyInputs.formula
            Item(
                name = Revenue,
                description = "Revenue",
                historicalValue = totRevnu,
                expression = formula
            )
        }
    }

    /**
     * Derive the going forward operating expenses in this model
     */
    private fun operatingExpenses(ctx: Context): Item {
        val latest = ctx.latestAnnual()
        val model = ctx.model
        val costGoodSold = latest.cost_good_sold.orZero()
        val totalOperExp = latest.tot_oper_exp.orZero()

        val cik = latest.comp_cik ?: error("company CIK cannot be found for ${model.symbol} on Zacks")
        val components = factComponentFinder.findFactComponents(
            cik = cik,
            conceptId = "us-gaap_OperatingExpenses"
        )
        val operatingExpenses = totalOperExp - costGoodSold

        /*
        find the one time facts
         */
        val oneTimeCharges = components
            .latestAnnual
            .filter { fact ->
                fact.explicitMembers.isEmpty() && fact.conceptName == "AssetImpairmentCharges"
            }

        return if (oneTimeCharges.isNotEmpty()) {
            val totalOneTime = oneTimeCharges.sumByDouble { (it.doubleValue.orZero()) / 1_000_000.0 }
            val fwrdOperatingExp = operatingExpenses - totalOneTime
            Item(
                name = OperatingExpense,
                description = "Operating Expense",
                historicalValue = operatingExpenses,
                expression = "$fwrdOperatingExp",
                commentaries = Commentary(
                    """
                    |Going forward operating expense will be ${'$'}${fwrdOperatingExp.fmtRound()} million,
                    |which excludes one time charges of ${'$'}${totalOneTime.fmtRound()} million
                """.trimMargin()
                )
            )
        } else {
            Item(
                name = OperatingExpense,
                description = "Operating Expense",
                historicalValue = operatingExpenses,
                expression = "$operatingExpenses",
            )
        }
    }

}
