package com.starburst.starburst.zacks.modelbuilder

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.fmtPct
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.fmtRound
import com.starburst.starburst.models.ReservedItemNames
import com.starburst.starburst.models.dataclasses.Commentary
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.zacks.dataclasses.ZacksFundamentalA

class BalanceSheetItemsBuilder {
    fun balanceSheetItems(fundamentalA: ZacksFundamentalA): List<Item> {
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
                name = ReservedItemNames.CurrentAsset,
                commentaries = Commentary("Current asset is ${caRatio.fmtPct()} of total asset"),
                historicalValue = totCurrAsset,
                expression = "$caRatio * ${ReservedItemNames.TotalAsset}",
            ),
            Item(
                name = ReservedItemNames.PropertyPlanetAndEquipement,
                commentaries = Commentary("PP&E is ${ppeRatio.fmtPct()} of total asset"),
                historicalValue = netPropPlantEquip,
                expression = "$ppeRatio * ${ReservedItemNames.TotalAsset}",
            ),
            Item(
                name = ReservedItemNames.LongTermAsset,
                commentaries = Commentary("LT Asset is ${ltaRatio.fmtPct()}% of total asset"),
                // y = px / (1-p); p = target % of total asset, x = everything except long-term asset
                // avoid circular reference
                historicalValue = totLtermAsset,
                expression = "$ltaRatio * ${ReservedItemNames.TotalAsset} + ${ReservedItemNames.PropertyPlanetAndEquipement}",
            ),
            Item(
                name = ReservedItemNames.TotalAsset,
                commentaries = Commentary("Total asset is ${totalAssetOverRevenue.fmtRound()}x of revenue"),
                historicalValue = totAsset,
                expression = "$totalAssetOverRevenue * ${ReservedItemNames.Revenue}"
            ),
            /*
            Liabilities
             */
            Item(
                name = ReservedItemNames.CurrentLiability,
                historicalValue = totCurrLiab,
                commentaries = Commentary("Current liability is ${clRatio.fmtPct()} of total liability"),
                expression = "$clRatio*${ReservedItemNames.TotalLiability}",
            ),
            Item(
                name = ReservedItemNames.LongTermDebt,
                historicalValue = totLtermDebt,
                commentaries = Commentary("Long-term debt liability is ${ltdRatio.fmtPct()} of total liability"),
                expression = "$ltdRatio*${ReservedItemNames.TotalLiability}",
            ),
            Item(
                name = ReservedItemNames.LongTermLiability,
                historicalValue = totLtermLiab,
                commentaries = Commentary("Long-term liability is ${ltlRatio.fmtPct()} of total liability"),
                expression = "$ltlRatio*${ReservedItemNames.TotalLiability} + ${ReservedItemNames.LongTermDebt}",
            ),
            Item(
                name = ReservedItemNames.TotalLiability,
                historicalValue = totLiab,
                commentaries = Commentary("Total liability is ${totalLiabilityOverRevenue.fmtRound()}x of revenue"),
                expression = "$totalLiabilityOverRevenue * ${ReservedItemNames.Revenue}"
            ),
            /*
            Shareholders Equity
             */
            Item(
                name = ReservedItemNames.ShareholdersEquity,
                historicalValue = totShareHolderEquity,
                expression = "${ReservedItemNames.TotalAsset} - ${ReservedItemNames.TotalLiability}"
            ),
            Item(
                name = ReservedItemNames.SharesOutstanding,
                historicalValue = avgBShares,
                expression = "$avgBShares"
            ),
        )
    }

}