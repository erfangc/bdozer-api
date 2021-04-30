package com.bdozer.revenuemodeler

import com.bdozer.models.dataclasses.Discrete
import com.bdozer.revenuemodeler.dataclasses.RevenueDriverType
import com.bdozer.revenuemodeler.dataclasses.RevenueModel
import com.bdozer.stockanalysis.StockAnalysisService
import com.bdozer.zacks.se.ZacksEstimatesService
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.deleteOneById
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.stereotype.Service

/**
 * [RevenueModeler] model revenues by creating a linear combination
 * of various driving factors
 */
@Service
class RevenueModeler(
    mongoDatabase: MongoDatabase,
    private val zacksEstimatesService: ZacksEstimatesService,
    private val stockAnalysisService: StockAnalysisService,
) {

    val col = mongoDatabase.getCollection<RevenueModel>()

    fun modelRevenue(revenueModel: RevenueModel): Discrete {
        return when (revenueModel.revenueDriverType) {
            RevenueDriverType.AverageRevenuePerUserTimesActiveUser -> {

                val ticker = stockAnalysisService
                    .getStockAnalysis(revenueModel.stockAnalysisId)
                    ?.ticker
                    ?: error("...")

                val zackEstimates = zacksEstimatesService.revenueProjections(ticker = ticker)
                val terminalYearActiveUser = revenueModel.terminalYearActiveUser
                val terminalYearAverageRevenuePerUser = revenueModel.terminalYearAverageRevenuePerUser
                val terminalYear = revenueModel.terminalYear

                /*
                see if we can find Zack's estimate for that year
                 */
                val finalZacksEstimate = zackEstimates.formulas[terminalYear].toString().toDoubleOrNull()

                TODO()
            }
            RevenueDriverType.ZacksEstimates -> {
                val ticker = stockAnalysisService.getStockAnalysis(revenueModel.stockAnalysisId)?.ticker
                    ?: error("...")
                zacksEstimatesService.revenueProjections(ticker = ticker)
            }
            else -> TODO()
        }
    }

    fun getRevenueModel(id: String): RevenueModel? {
        return col.findOneById(id)
    }

    fun deleteRevenueModel(id: String) {
        col.deleteOneById(id)
    }

    fun saveRevenueModel(revenueModel: RevenueModel) {
        return col.save(revenueModel)
    }

}