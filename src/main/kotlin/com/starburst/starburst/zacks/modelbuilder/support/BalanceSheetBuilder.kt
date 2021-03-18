package com.starburst.starburst.zacks.modelbuilder.support

import com.starburst.starburst.DoubleExtensions.fmtPct
import com.starburst.starburst.DoubleExtensions.fmtRound
import com.starburst.starburst.models.Utility.CurrentAsset
import com.starburst.starburst.models.Utility.CurrentLiability
import com.starburst.starburst.models.Utility.LongTermAsset
import com.starburst.starburst.models.Utility.LongTermDebt
import com.starburst.starburst.models.Utility.LongTermLiability
import com.starburst.starburst.models.Utility.PropertyPlanetAndEquipement
import com.starburst.starburst.models.Utility.Revenue
import com.starburst.starburst.models.Utility.ShareholdersEquity
import com.starburst.starburst.models.Utility.SharesOutstanding
import com.starburst.starburst.models.Utility.TotalAsset
import com.starburst.starburst.models.Utility.TotalLiability
import com.starburst.starburst.models.dataclasses.Commentary
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.zacks.dataclasses.BalanceSheet
import com.starburst.starburst.zacks.dataclasses.Context
import org.springframework.stereotype.Service

@Service
class BalanceSheetBuilder {

    fun balanceSheetItems(ctx: Context): BalanceSheet {
        val latest = ctx.latestAnnual()
        val totRevnu = latest.tot_revnu ?: 0.0
        val totCurrAsset = latest.tot_curr_asset ?: 0.0
        val netPropPlantEquip = latest.net_prop_plant_equip ?: 0.0
        val totLtermAsset = latest.tot_lterm_asset ?: 0.0
        val totAsset = latest.tot_asset ?: 0.0

        val caRatio = totCurrAsset / totAsset
        val ppeRatio = netPropPlantEquip / totAsset
        val ltaRatio = (totLtermAsset - netPropPlantEquip) / totAsset

        val totalAssetOverRevenue = totAsset / totRevnu

        val totCurrLiab = latest.tot_curr_liab ?: 0.0
        val totLiab = latest.tot_liab ?: 0.0
        val totLtermDebt = latest.tot_lterm_debt ?: 0.0
        val totLtermLiab = latest.tot_lterm_liab ?: 0.0
        val totShareHolderEquity = latest.tot_share_holder_equity ?: 0.0

        val clRatio = totCurrLiab / totLiab
        val ltdRatio = totLtermDebt / totLiab
        val ltlRatio = (totLtermLiab - totLtermDebt) / totLiab

        val totalLiabilityOverRevenue = totRevnu / totLiab
        val avgBShares = latest.avg_b_shares ?: 0.0

        val items = listOf(
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
                expression = "$clRatio*${TotalLiability}",
            ),
            Item(
                name = LongTermDebt,
                historicalValue = totLtermDebt,
                commentaries = Commentary("Long-term debt liability is ${ltdRatio.fmtPct()} of total liability"),
                expression = "$ltdRatio*${TotalLiability}",
            ),
            Item(
                name = LongTermLiability,
                historicalValue = totLtermLiab,
                commentaries = Commentary("Long-term liability is ${ltlRatio.fmtPct()} of total liability"),
                expression = "$ltlRatio*${TotalLiability} + $LongTermDebt",
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
        return BalanceSheet(items = items)
    }

}