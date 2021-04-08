package com.starburst.starburst.stockanalyzer.analyzers

import com.starburst.starburst.extensions.DoubleExtensions.fmtPct
import com.starburst.starburst.models.dataclasses.Commentary
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.stockanalyzer.dataclasses.StockAnalysis2

/**
 * This [com.starburst.starburst.stockanalyzer.common.StockAnalyzer]
 * models costs by looking back to historical periods where operations are stable and positive
 * instead of estimating costs as a percentage of most recent year
 *
 * The aim is to model situations where unexpected shocks caused large declines
 */
class Normal(
    dataProvider: StockAnalyzerDataProvider,
    originalStockAnalysis: StockAnalysis2
) : AbstractStockAnalyzer(dataProvider, originalStockAnalysis) {

    override fun processOperatingCostItem(item: Item): Item {
        return itemAsPercentOfRevenue(item)
    }

    private fun itemAsPercentOfRevenue(item: Item): Item {
        val ts = timeSeriesVsRevenue(conceptName = item.name)
        /*
        for this scenario we remove the most recent period
         */
        val pctOfRev = ts.map { it.second / it.first }
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
