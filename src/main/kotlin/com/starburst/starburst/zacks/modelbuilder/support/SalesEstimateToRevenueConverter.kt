package com.starburst.starburst.zacks.modelbuilder.support

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.fmtPct
import com.starburst.starburst.models.Utility
import com.starburst.starburst.models.dataclasses.Discrete
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.ItemType
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.zacks.se.ZacksEstimatesService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SalesEstimateToRevenueConverter(
    private val zacksEstimatesService: ZacksEstimatesService,
) {

    private val log = LoggerFactory.getLogger(SalesEstimateToRevenueConverter::class.java)

    fun convert(model: Model, latestActualRevenue: Double): Item {

        /*
        turn sales estimates into revenue assumptions
         */
        val totalPeriod = model.periods
        val ticker = model.symbol ?: error("...")
        val estimates = zacksEstimatesService.getZacksSaleEstimates(ticker)

        /*
        we find the first estimate with a time > today
         */
        val relevantEstimates = estimates
            .filter { estimate ->
                val isFuture = estimate.per_end_date?.isAfter(LocalDate.now()) == true
                val isAnnual = estimate.per_type == "A"
                val hasMedianEst = estimate.sales_median_est != null
                isFuture && isAnnual && hasMedianEst
            }
            .sortedBy { it.per_end_date }

        /*
        short circuit the process if estimates are not found
         */
        if (relevantEstimates.isEmpty()) {
            error(
                "sales estimates cannot be found for $ticker that is annual and is in the future, " +
                        "found raw estimates ${estimates.size}"
            )
        }

        /*
        find the years for which we do not have an estimate, and smooth out the last period's
        growth to terminal growth of 0.0 using extrapolation
         */
        val extrapolatedRevenueEstimates = if (relevantEstimates.size < totalPeriod) {
            /*
            figure out the growth rate implied by the most estimate
             */
            val lastEstimate = relevantEstimates.last().sales_median_est ?: 0.0
            val secondToLastEsitmate = if (relevantEstimates.size >= 2) {
                relevantEstimates[relevantEstimates.size - 2].sales_median_est ?: 0.0
            } else {
                latestActualRevenue
            }
            val growthRate = (lastEstimate / secondToLastEsitmate) - 1

            /*
            extrapolation period
             */
            val extrapolationPeriod = totalPeriod - relevantEstimates.size
            val extrapolationSlope = growthRate / extrapolationPeriod
            val extrapolatedGrowthRates = (1..extrapolationPeriod).map { period ->
                growthRate - extrapolationSlope * period
            }
            val extrapolatedRevenues = extrapolatedGrowthRates
                .fold(emptyList<Double>()) { acc, growthRate ->
                    val extrapolatedRevenue = if (acc.isEmpty()) {
                        lastEstimate * (1 + growthRate)
                    } else {
                        acc.last() * (1 + growthRate)
                    }
                    acc + extrapolatedRevenue
                }
            log.info(
                "Extrapolation required for revenue projection, " +
                        "lastEstimate=$lastEstimate, " +
                        "extrapolationPeriod=$extrapolationPeriod, " +
                        "extrapolationSlope=${extrapolationSlope.fmtPct()}, " +
                        "lastExplicitlyEstimatedGrowthRate=${growthRate.fmtPct()}"
            )
            extrapolatedRevenues
        } else {
            emptyList()
        }

        val explicitRevenueEstimates = relevantEstimates.mapNotNull { it.sales_median_est }

        val revenueEstimates = (explicitRevenueEstimates + extrapolatedRevenueEstimates)
            .mapIndexed { idx, estimate ->
                val period = idx + 1
                period to "$estimate"
            }
            .toMap()

        return Item(
            name = Utility.Revenue,
            description = "Revenue",
            historicalValue = latestActualRevenue,
            type = ItemType.Discrete,
            discrete = Discrete(
                formulas = revenueEstimates
            )
        )
    }

}