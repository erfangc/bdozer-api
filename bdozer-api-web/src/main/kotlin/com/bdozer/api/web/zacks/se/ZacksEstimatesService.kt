package com.bdozer.api.web.zacks.se

import bdozer.api.common.extensions.DoubleExtensions.orZero
import com.bdozer.api.models.dataclasses.Item
import com.bdozer.api.models.dataclasses.ManualProjection
import com.bdozer.api.models.dataclasses.ManualProjections
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ZacksEstimatesService(mongoDatabase: MongoDatabase) {

    private val zacksSalesEstimates = mongoDatabase.getCollection<ZacksSalesEstimatesWrapper>()

    fun getZacksSaleEstimates(ticker: String): List<ZacksSalesEstimates> {
        return zacksSalesEstimates
            .find(ZacksSalesEstimatesWrapper::content / ZacksSalesEstimates::ticker eq ticker)
            .map { it.content }
            .toList()
    }

    /**
     * Creates a revenue [Item] using Zack's estimates
     */
    fun revenueProjections(ticker: String): ManualProjections {
        /*
        turn sales estimates into revenue assumptions
         */
        val estimates = getZacksSaleEstimates(ticker)

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

        val manualProjections = relevantEstimates
            .map { zacksSalesEstimates ->
                val year = zacksSalesEstimates.per_end_date?.year ?: error("...")
                ManualProjection(
                    fiscalYear = year,
                    value = zacksSalesEstimates.sales_median_est?.times(1_000_000.0).orZero(),
                )
            }
        return ManualProjections(manualProjections)
    }

}