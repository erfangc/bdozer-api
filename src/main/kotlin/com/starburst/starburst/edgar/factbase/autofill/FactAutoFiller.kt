package com.starburst.starburst.edgar.factbase.autofill

import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.autofill.dataclasses.PercentOfRevenueAutoFill
import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.extensions.DoubleExtensions.orZero
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.models.dataclasses.PercentOfRevenue
import org.springframework.stereotype.Service

@Service
class FactAutoFiller(private val factBase: FactBase) {

    /**
     * Percent of Revenue autofill options
     * @param factId the factId to autofill based on
     * @param model the [Model]
     */
    fun getPercentOfRevenueAutoFills(factId: String, model: Model): List<PercentOfRevenueAutoFill> {

        val revenueFactId = model
            .incomeStatementItems
            .find { item -> item.name == model.totalRevenueConceptName }
            ?.historicalValue?.factId ?: error("unable to determine revenue factId")

        val timeSeries = factBase.getFactTimeSeries(factId)
        val revenueTimeSeries = factBase.getFactTimeSeries(revenueFactId)

        val pairs = toPairs(revenueTimeSeries.fyFacts, timeSeries.fyFacts)
        val average = pairs.map { it[1] / it[0] }.average()
        val latest = pairs.last().let { it[1] / it[0] }

        return listOf(
            PercentOfRevenueAutoFill(
                label = "Historical Average",
                percentOfRevenue = PercentOfRevenue(percentOfRevenue = average)
            ),
            PercentOfRevenueAutoFill(
                label = "Latest",
                percentOfRevenue = PercentOfRevenue(percentOfRevenue = latest)
            ),
        )
    }

    private fun toPairs(x: List<Fact>, y: List<Fact>): Array<DoubleArray> {
        val lookup = y.associateBy { it.documentPeriodEndDate }
        return x
            .sortedBy { it.documentPeriodEndDate }
            .map { doubleArrayOf(it.doubleValue.orZero(), lookup[it.documentPeriodEndDate]?.doubleValue.orZero()) }
            .toTypedArray()
    }

}