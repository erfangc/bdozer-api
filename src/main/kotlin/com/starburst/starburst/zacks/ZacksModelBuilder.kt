package com.starburst.starburst.zacks

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.fmtPct
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.fmtRound
import com.starburst.starburst.models.ReservedItemNames.CostOfGoodsSold
import com.starburst.starburst.models.ReservedItemNames.CurrentAsset
import com.starburst.starburst.models.ReservedItemNames.CurrentLiability
import com.starburst.starburst.models.ReservedItemNames.DiscountFactor
import com.starburst.starburst.models.ReservedItemNames.EarningsPerShare
import com.starburst.starburst.models.ReservedItemNames.GrossProfit
import com.starburst.starburst.models.ReservedItemNames.LongTermAsset
import com.starburst.starburst.models.ReservedItemNames.LongTermDebt
import com.starburst.starburst.models.ReservedItemNames.LongTermLiability
import com.starburst.starburst.models.ReservedItemNames.NetIncome
import com.starburst.starburst.models.ReservedItemNames.NonOperatingExpense
import com.starburst.starburst.models.ReservedItemNames.OperatingExpense
import com.starburst.starburst.models.ReservedItemNames.OperatingIncome
import com.starburst.starburst.models.ReservedItemNames.PresentValuePerShare
import com.starburst.starburst.models.ReservedItemNames.PretaxIncome
import com.starburst.starburst.models.ReservedItemNames.PropertyPlanetAndEquipement
import com.starburst.starburst.models.ReservedItemNames.Revenue
import com.starburst.starburst.models.ReservedItemNames.ShareholdersEquity
import com.starburst.starburst.models.ReservedItemNames.SharesOutstanding
import com.starburst.starburst.models.ReservedItemNames.TaxExpense
import com.starburst.starburst.models.ReservedItemNames.TerminalValuePerShare
import com.starburst.starburst.models.ReservedItemNames.TotalAsset
import com.starburst.starburst.models.ReservedItemNames.TotalLiability
import com.starburst.starburst.models.dataclasses.Commentary
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.Model
import com.vhl.blackmo.grass.dsl.grass
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@ExperimentalStdlibApi
@Service
class ZacksModelBuilder {

    private val zacksFundamentalAs: List<ZacksFundamentalA>
    private val fileName = "/Users/erfangchen/Downloads/ZACKS_FC_addc6c96afcc63aaedeb3dae8c933d5a.csv"
    private val log = LoggerFactory.getLogger(ZacksModelBuilder::class.java)

    init {
        val csvContents = csvReader().readAllWithHeader(File(fileName))
        zacksFundamentalAs = grass<ZacksFundamentalA> { dateFormat = "M/d/yy" }.harvest(csvContents)
        log.info("Loaded ${zacksFundamentalAs.size} ${zacksFundamentalAs.javaClass.simpleName} from $fileName")
    }

    /**
     * Build a model using Zacks Fundamental A data
     * for the given ticker
     */
    fun buildModel(ticker: String): Model {

        val fundamentalA = findZacksFundamentalA(ticker)

        val incomeStatementItems = incomeStatementItems(fundamentalA)
        val balanceSheetItems = balanceSheetItems(fundamentalA)

        val model = Model(
            symbol = ticker,
            name = fundamentalA.comp_name ?: "N/A",
            incomeStatementItems = incomeStatementItems,
            balanceSheetItems = balanceSheetItems,
            cashFlowStatementItems = listOf(),
            otherItems = listOf(),
        )

        val periods = model.periods
        val discountRate = (model.equityRiskPremium * model.beta) + model.riskFreeRate

        val otherItems = listOf(
            Item(
                name = DiscountFactor,
                expression = "1 / (1.0 + $discountRate)^period"
            ),
            Item(
                name = EarningsPerShare,
                expression = "$NetIncome / $SharesOutstanding"
            ),
            Item(
                name = TerminalValuePerShare,
                expression = "if(period=$periods,$EarningsPerShare * ${model.terminalFcfMultiple},0.0)"
            ),
            Item(
                name = PresentValuePerShare,
                // TODO figure out the correct numerator if we can actually derive FCF
                expression = "$DiscountFactor * ($EarningsPerShare + $TerminalValuePerShare)"
            )
        )

        return model.copy(otherItems = otherItems)

    }

    private fun balanceSheetItems(fundamentalA: ZacksFundamentalA): List<Item> {
        val totRevnu = fundamentalA.tot_revnu ?: 0.0
        val totCurrAsset = fundamentalA.tot_curr_asset ?: 0.0
        val netPropPlantEquip = fundamentalA.net_prop_plant_equip ?: 0.0
        val totLtermAsset = fundamentalA.tot_lterm_asset ?: 0.0
        val totAsset = fundamentalA.tot_asset ?: 0.0

        val caRatio = totCurrAsset / totAsset
        val ppeRatio = netPropPlantEquip / totAsset
        val ltaRatio = (totLtermAsset - netPropPlantEquip) / totAsset

        val totalAssetOverRevenue = totAsset / totRevnu

        val totCurrLiab = fundamentalA.tot_curr_liab ?: 0.0
        val totLiab = fundamentalA.tot_liab ?: 0.0
        val totLtermDebt = fundamentalA.tot_lterm_debt ?: 0.0
        val totLtermLiab = fundamentalA.tot_lterm_liab ?: 0.0
        val totShareHolderEquity = fundamentalA.tot_share_holder_equity ?: 0.0

        val clRatio = totCurrLiab / totLiab
        val ltdRatio = totLtermDebt / totLiab
        val ltlRatio = (totLtermLiab - totLtermDebt) / totLiab

        val totalLiabilityOverRevenue = totRevnu / totLiab
        val avgBShares = fundamentalA.avg_b_shares ?: 0.0

        return listOf(
            /*
            Assets
             */
            Item(
                name = CurrentAsset,
                commentaries = Commentary("Current asset is ${caRatio.fmtPct()} of total asset"),
                historicalValue = totCurrAsset,
                expression = "$caRatio * $TotalAsset",
            ),
            Item(
                name = PropertyPlanetAndEquipement,
                commentaries = Commentary("PP&E is ${ppeRatio.fmtPct()} of total asset"),
                historicalValue = netPropPlantEquip,
                expression = "$ppeRatio * $TotalAsset",
            ),
            Item(
                name = LongTermAsset,
                commentaries = Commentary("LT Asset is ${ltaRatio.fmtPct()}% of total asset"),
                // y = px / (1-p); p = target % of total asset, x = everything except long-term asset
                // avoid circular reference
                historicalValue = totLtermAsset,
                expression = "$ltaRatio * $TotalAsset + $PropertyPlanetAndEquipement",
            ),
            Item(
                name = TotalAsset,
                commentaries = Commentary("Total asset is ${totalAssetOverRevenue.fmtRound()}x of revenue"),
                historicalValue = totAsset,
                expression = "$totalAssetOverRevenue * $Revenue"
            ),
            /*
            Liabilities
             */
            Item(
                name = CurrentLiability,
                historicalValue = totCurrLiab,
                commentaries = Commentary("Current liability is ${clRatio.fmtPct()} of total liability"),
                expression = "$clRatio*$TotalLiability",
            ),
            Item(
                name = LongTermDebt,
                historicalValue = totLtermDebt,
                commentaries = Commentary("Long-term debt liability is ${ltdRatio.fmtPct()} of total liability"),
                expression = "$ltdRatio*$TotalLiability",
            ),
            Item(
                name = LongTermLiability,
                historicalValue = totLtermLiab,
                commentaries = Commentary("Long-term liability is ${ltlRatio.fmtPct()} of total liability"),
                expression = "$ltlRatio*$TotalLiability + $LongTermDebt",
            ),
            Item(
                name = TotalLiability,
                historicalValue = totLiab,
                commentaries = Commentary("Total liability is ${totalLiabilityOverRevenue.fmtRound()}x of revenue"),
                expression = "$totalLiabilityOverRevenue * $Revenue"
            ),
            /*
            Shareholders Equity
             */
            Item(
                name = ShareholdersEquity,
                historicalValue = totShareHolderEquity,
                expression = "$TotalAsset - $TotalLiability"
            ),
            Item(
                name = SharesOutstanding,
                historicalValue = avgBShares,
                expression = "$avgBShares"
            ),
        )
    }

    private fun findZacksFundamentalA(ticker: String) = (zacksFundamentalAs
        .find { it.ticker == ticker }
        ?: error("Zacks fundamentals for $ticker not found"))

    private fun incomeStatementItems(fundamentalA: ZacksFundamentalA): List<Item> {
        val zacksRevenue = fundamentalA.tot_revnu ?: 0.0

        val revenue = Item(
            name = Revenue,
            description = "Revenue",
            historicalValue = zacksRevenue,
            expression = "${fundamentalA.tot_revnu ?: 0.0}",
        )

        val zacksCogs = fundamentalA.cost_good_sold ?: 0.0
        val cogsPctOfRevenue = zacksCogs / zacksRevenue
        val cogs = Item(
            name = CostOfGoodsSold,
            description = "Cost of Goods",
            historicalValue = zacksCogs,
            commentaries = Commentary(commentary = "Cost of goods sold has been ${cogsPctOfRevenue.fmtPct()}"),
            expression = "$cogsPctOfRevenue * $Revenue",
        )
        val grossProfit = Item(
            name = GrossProfit,
            description = "Gross Profit",
            historicalValue = fundamentalA.gross_profit ?: 0.0,
            expression = "$Revenue - $CostOfGoodsSold",
        )
        val operatingExpense = Item(
            name = OperatingExpense,
            description = "Operating Expense",
            historicalValue = fundamentalA.tot_oper_exp ?: 0.0,
            // TODO these need to be regressed against historical to determine variable vs. fixed component
            expression = "${fundamentalA.tot_oper_exp ?: 0.0}",
        )
        val operatingIncome = Item(
            name = OperatingIncome,
            description = "Operating Income",
            historicalValue = fundamentalA.oper_income ?: 0.0,
            expression = "$GrossProfit - $OperatingExpense",
        )
        val nonOperatingExpense = Item(
            name = NonOperatingExpense,
            description = "Non-Operating Expense",
            historicalValue = fundamentalA.tot_non_oper_income_exp ?: 0.0,
            // TODO based on historical data determine whether to use average or mark them as one time
            expression = "${fundamentalA.tot_non_oper_income_exp ?: 0.0}",
        )

        val zacksPretaxIncome = fundamentalA.pre_tax_income ?: 0.0
        val pretaxIncome = Item(
            name = PretaxIncome,
            description = "Pretax Income",
            historicalValue = zacksPretaxIncome,
            expression = "$OperatingIncome - $NonOperatingExpense",
        )

        val zacksTaxExpense = zacksPretaxIncome - (fundamentalA.net_income_loss_share_holder ?: 0.0)
        val taxRate = 0.1.coerceAtLeast(zacksTaxExpense / zacksPretaxIncome)

        val taxExpense = Item(
            name = TaxExpense,
            description = "Tax Expense",
            historicalValue = zacksTaxExpense,
            expression = "$taxRate * $PretaxIncome",
            commentaries = Commentary("Pretax income is taxed at ${taxRate.fmtPct()}")
        )

        val netIncome = Item(
            name = NetIncome,
            description = "Net Income",
            historicalValue = fundamentalA.net_income_loss_share_holder ?: 0.0,
            expression = "$PretaxIncome - $TaxExpense"
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
