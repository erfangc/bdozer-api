package com.starburst.starburst.modelbuilder.templates

import com.starburst.starburst.DoubleExtensions.fmtPct
import com.starburst.starburst.modelbuilder.common.AbstractStockAnalyzer
import com.starburst.starburst.modelbuilder.common.StockAnalyzerDataProvider
import com.starburst.starburst.models.dataclasses.Commentary
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.modelbuilder.support.Context
import com.starburst.starburst.modelbuilder.support.SalesEstimateToRevenueConverter
import com.starburst.starburst.zacks.se.ZacksEstimatesService

class EarningsRecoveryAnalyzer(
    private val zacksEstimatesService: ZacksEstimatesService,
    dataProvider: StockAnalyzerDataProvider,
) : AbstractStockAnalyzer(dataProvider) {

    override fun processOperatingCostItem(item: Item): Item {
        return itemAsPercentOfRevenue(item)
    }

    override fun processTotalRevenueItem(item: Item): Item {
        val model = emptyModel()
        val ticker = filingEntity.tradingSymbol ?: error("...")
        val salesEstimates = zacksEstimatesService.getZacksSaleEstimates(ticker)
        val ctx = Context(
            model = model,
            zacksSalesEstimates = salesEstimates,
        )
        val converter = SalesEstimateToRevenueConverter(ctx = ctx)
        val revenueItem = converter.convert(item.historicalValue?.value ?: error("..."))
        val discrete = revenueItem.discrete
        return revenueItem
            .copy(
                name = item.name,
                historicalValue = item.historicalValue,
                discrete = discrete?.copy(
                    formulas = discrete
                        .formulas
                        .mapValues { (_, v) ->
                            (v.toDouble() * 1_000_000.0).toString()
                        }
                )
            )
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
