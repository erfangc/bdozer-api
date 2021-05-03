package com.bdozer.api.web.revenuemodeler

import com.bdozer.api.web.extensions.DoubleExtensions.orZero
import bdozer.api.common.model.ManualProjection
import bdozer.api.common.model.ManualProjections
import com.bdozer.api.web.revenuemodeler.dataclasses.ModelRevenueRequest
import com.bdozer.api.web.revenuemodeler.dataclasses.RevenueDriverType
import com.bdozer.api.web.revenuemodeler.dataclasses.RevenueModel
import com.bdozer.api.web.stockanalysis.StockAnalysisService
import com.bdozer.api.web.zacks.se.ZacksEstimatesService
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.deleteOneById
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.stereotype.Service
import java.time.LocalDate

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

    fun modelRevenue(
        request: ModelRevenueRequest
    ): ManualProjections {
        val (revenueModel, model) = request
        return when (revenueModel.revenueDriverType) {
            RevenueDriverType.AverageRevenuePerUserTimesActiveUser -> {
                val totalRevenueItem = model.item(model.totalRevenueConceptName)

                val fy0Revenue = totalRevenueItem?.historicalValue?.value.orZero()
                val fy0 = totalRevenueItem?.historicalValue?.documentFiscalYearFocus
                    ?: totalRevenueItem?.historicalValue?.documentPeriodEndDate?.let { LocalDate.parse(it).year }
                    ?: LocalDate.now().year

                val fyTerminal = revenueModel.terminalFiscalYear ?: error("terminalFiscalYear must be specified")
                val terminalYearActiveUser = revenueModel.terminalYearActiveUser.orZero()
                val terminalYearAverageRevenuePerUser = revenueModel.terminalYearAverageRevenuePerUser.orZero()
                val fyTerminalRevenue = terminalYearActiveUser * terminalYearAverageRevenuePerUser

                val slope = (fyTerminalRevenue - fy0Revenue) / (fyTerminal - fy0)

                /*
                project revenue from fy0 -> terminal year
                each period's revenue
                 */
                val manualProjections = (fy0..fyTerminal).map { fiscalYear ->
                    val value = (fiscalYear - fy0) * slope + fy0Revenue
                    ManualProjection(fiscalYear = fiscalYear, value = value)
                }

                ManualProjections(manualProjections)
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