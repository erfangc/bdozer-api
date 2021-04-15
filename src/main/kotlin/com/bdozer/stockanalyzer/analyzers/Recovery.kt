package com.bdozer.stockanalyzer.analyzers

import com.bdozer.extensions.DoubleExtensions.fmtPct
import com.bdozer.models.dataclasses.Commentary
import com.bdozer.models.dataclasses.Item
import com.bdozer.stockanalyzer.dataclasses.StockAnalysis2

/**
 * This [com.bdozer.stockanalyzer.common.StockAnalyzer]
 * models costs by looking back to historical periods where operations are stable and positive
 * instead of estimating costs as a percentage of most recent year
 *
 * The aim is to model situations where unexpected shocks caused large declines
 */
class Recovery(dataProvider: StockAnalyzerDataProvider, stockAnalysis: StockAnalysis2) :
    AbstractStockAnalyzer(dataProvider, stockAnalysis) {

    override fun processOperatingCostItem(item: Item): Item {
        return itemAsPercentOfRevenue(item)
    }

    private fun itemAsPercentOfRevenue(item: Item): Item {
        val ts = timeSeriesVsRevenue(conceptName = item.name)
        /*
        for this scenario we remove the most recent period
         */
        val pctOfRev = ts.subList(0, ts.size - 1).map { it.second / it.first }
        return if (pctOfRev.isNotEmpty()) {
            val pct = pctOfRev.average()
            item.copy(
                formula = "$pct * $totalRevenueConceptName",
                commentaries = Commentary(
                    commentary = "Using historical average of ${pct.fmtPct()}, excluding instances when it was >100%"
                )
            )
        } else {
            // perpetuate current
            val pct = ts.last().second / ts.last().first
            item.copy(
                formula = "$pct * $totalRevenueConceptName",
                commentaries = Commentary(
                    commentary = "Insufficient history of clean data, applying the most recent revenue percentage ${pct.fmtPct()} going forward"
                )
            )
        }
    }

}
