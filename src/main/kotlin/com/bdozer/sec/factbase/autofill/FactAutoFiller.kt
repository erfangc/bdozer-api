package com.bdozer.sec.factbase.autofill

import com.bdozer.extensions.DoubleExtensions.orZero
import com.bdozer.models.dataclasses.FixedCost
import com.bdozer.models.dataclasses.Model
import com.bdozer.models.dataclasses.PercentOfRevenue
import com.bdozer.sec.factbase.autofill.dataclasses.FixedCostAutoFill
import com.bdozer.sec.factbase.autofill.dataclasses.PercentOfAnotherItemAutoFill
import com.bdozer.sec.factbase.autofill.dataclasses.PercentOfRevenueAutoFill
import com.bdozer.sec.factbase.core.FactBase
import com.bdozer.sec.factbase.dataclasses.AggregatedFact
import com.bdozer.sec.factbase.dataclasses.Fact
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * [FactAutoFiller] creates auto fill
 */
@Service
class FactAutoFiller(private val factBase: FactBase) {

    private val log = LoggerFactory.getLogger(FactAutoFiller::class.java)

    /**
     * Percent of Revenue autofill options
     *
     * @param itemName the factId to autofill based on
     * @param model the [Model]
     */
    fun getPercentOfRevenueAutoFills(
        itemName: String,
        model: Model,
    ): List<PercentOfRevenueAutoFill> {
        try {
            val item = model.item(itemName) ?: error("unable to find item $itemName")

            val revenueFactId = model
                .item(model.totalRevenueConceptName)
                ?.historicalValue
                ?.factId ?: error("unable to determine revenue factId")

            val factIds = item.historicalValue?.factId?.let { listOf(it) }
                ?: item.historicalValue?.factIds
                ?: error("cannot determine factId on item $itemName")

            val timeSeries = factBase.getAnnualTimeSeries(factIds)

            val revenueTimeSeries = factBase.getAnnualTimeSeries(revenueFactId)
            val pairs = toPairs(revenueTimeSeries, timeSeries)
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
        } catch (e: Exception) {
            log.error("error occurred", e)
            return emptyList()
        }
    }

    /**
     * Percent of Revenue autofill options
     *
     * @param factId the factId to autofill based on
     * @param model the [Model]
     */
    fun getFixedCostAutoFills(itemName: String, model: Model): List<FixedCostAutoFill> {
        try {
            val item = model.item(itemName) ?: error("...")

            val factIds = item.historicalValue?.factId?.let { listOf(it) }
                ?: item.historicalValue?.factIds
                ?: error("cannot determine factId on item $itemName")

            val timeSeries = factBase.getAnnualTimeSeries(factIds)

            val values = timeSeries.map { it.value.orZero() }

            val average = values.average()
            val latest = values.last().orZero()

            return listOf(
                FixedCostAutoFill(
                    label = "Historical Average",
                    fixedCost = FixedCost(cost = average)
                ),
                FixedCostAutoFill(
                    label = "Latest",
                    fixedCost = FixedCost(cost = latest)
                ),
            )
        } catch (e: Exception) {
            log.error("error occurred", e)
            return emptyList()
        }
    }

    fun getPercentOfAnotherItemAutoFills(
        itemName: String,
        dependentItemName: String,
        model: Model
    ): List<PercentOfAnotherItemAutoFill> {

        TODO("Not yet implemented")
    }

    private fun toPairs(x: List<Fact>, y: List<AggregatedFact>): Array<DoubleArray> {
        val lookup = y.associateBy { it.documentPeriodEndDate }
        return x
            .sortedBy { it.documentPeriodEndDate }
            .map { doubleArrayOf(it.doubleValue.orZero(), lookup[it.documentPeriodEndDate]?.value.orZero()) }
            .toTypedArray()
    }

}