package com.starburst.starburst.zacks.modelbuilder.support

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.fmtPct
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.fmtRound
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
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.zacks.fa.ZacksFundamentalA
import com.starburst.starburst.zacks.fa.ZacksFundamentalAService
import com.starburst.starburst.zacks.modelbuilder.keyinputs.KeyInputs
import com.starburst.starburst.zacks.modelbuilder.keyinputs.KeyInputsProvider
import org.springframework.stereotype.Service

@Service
class IncomeStatementItemsBuilder(
    private val keyInputsProvider: KeyInputsProvider,
    private val zacksFundamentalAService: ZacksFundamentalAService,
    private val salesEstimateToRevenueConverter: SalesEstimateToRevenueConverter,
    private val factComponentFinder: FactComponentFinder,
) {

    private fun Double?.orZ(): Double {
        return this ?: 0.0
    }

    private fun findFwrdCogsPCt(fundamentalAs: List<ZacksFundamentalA>): Double? {
        if (fundamentalAs.isEmpty()) {
            return null
        }
        val annualCogsPct = fundamentalAs.map { row ->
            row.cost_good_sold.orZ() / row.tot_revnu.orZ()
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
    private fun modelCostOfGoods(latest: ZacksFundamentalA): Item {
        val ticker = latest.ticker!!
        val fundamentalAs = zacksFundamentalAService.getZacksFundamentalAs(ticker)

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
        val earliestCogs = cogsHist.first() ?: 0.0
        val latestCogs = cogsHist.last() ?: 0.0
        val verb = if (latestCogs > earliestCogs) {
            "up"
        } else {
            "down"
        }

        return Item(
            name = CostOfGoodsSold,
            description = "Cost of Goods",
            historicalValue = latest.cost_good_sold.orZ(),
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

    fun incomeStatementItems(model: Model, latest: ZacksFundamentalA, keyInputs: KeyInputs? = null): List<Item> {

        /*
        Extract and prepare some basic inputs
         */
        val preTaxIncome = latest.pre_tax_income ?: 0.0
        val netIncomeLossShareHolder = latest.net_income_loss_share_holder ?: 0.0
        val totNonOperIncomeExp = latest.tot_non_oper_income_exp ?: 0.0

        /*
        Compute some basic metrics that will be used
        to drive balance sheet items
         */
        val costOfGoods = modelCostOfGoods(latest)
        val taxesPaid = preTaxIncome - netIncomeLossShareHolder
        val taxRate = (taxesPaid / preTaxIncome)
            .coerceAtLeast(0.08)
            .coerceAtMost(0.15)

        val revenue = keyInputsToRevenue(model, latest, keyInputs)
        val operatingExpenses = operatingExpenses(model, latest)

        return listOf(
            revenue,
            costOfGoods,
            Item(
                name = GrossProfit,
                description = "Gross Profit",
                historicalValue = latest.gross_profit ?: 0.0,
                expression = "$Revenue - $CostOfGoodsSold",
            ),
            operatingExpenses,
            Item(
                name = OperatingIncome,
                description = "Operating Income",
                historicalValue = latest.oper_income ?: 0.0,
                expression = "$GrossProfit-$OperatingExpense",
            ),
            Item(
                name = NonOperatingExpense,
                description = "Non-Operating Expense",
                historicalValue = latest.tot_non_oper_income_exp ?: 0.0,
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
                historicalValue = latest.net_income_loss_share_holder ?: 0.0,
                expression = "$PretaxIncome-$TaxExpense"
            ),
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
        model: Model,
        latest: ZacksFundamentalA,
        keyInputs: KeyInputs? = null
    ): Item {
        val ticker = latest.ticker ?: error("...")
        val keyInputs = keyInputs ?: keyInputsProvider.getKeyInputs(ticker)
        val totRevnu = latest.tot_revnu ?: 0.0
        return if (keyInputs == null) {
            salesEstimateToRevenueConverter.convert(model, totRevnu)
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
    private fun operatingExpenses(model: Model, latest: ZacksFundamentalA): Item {
        val costGoodSold = latest.cost_good_sold ?: 0.0
        val totalOperExp = latest.tot_oper_exp ?: 0.0

        val cik = latest.comp_cik ?: error("company CIK cannot be found for ${model.symbol} on Zacks")
        val components = factComponentFinder.factComponents(
            cik = cik,
            conceptId = "us-gaap_OperatingExpenses"
        )
        val operatingExpenses = totalOperExp - costGoodSold

        /*
        find the one time facts
         */
        val oneTimeCharges = components
            .latestAnnualFacts
            .filter { fact ->
                fact.explicitMembers.isEmpty() && fact.conceptName == "AssetImpairmentCharges"
            }

        return if (oneTimeCharges.isNotEmpty()) {
            val totalOneTime = oneTimeCharges.sumByDouble { (it.doubleValue ?: 0.0) / 1_000_000.0 }
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
