package com.bdozer.stockanalysis.cron

import com.bdozer.extensions.DoubleExtensions.orZero
import com.bdozer.iex.IEXService
import com.bdozer.stockanalysis.StockAnalysisService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.lang.Exception

@Service
class StockAnalysisCronJobs(
    private val stockAnalysisService: StockAnalysisService,
    private val iexService: IEXService
) {

    @Scheduled(cron = "0 0 9-17 * * MON-FRI")
    fun updatePrices() {
        stockAnalysisService.findStockAnalyses(limit = Int.MAX_VALUE).stockAnalyses.forEach {
            try {
                val stockAnalysis = stockAnalysisService.getStockAnalysis(it._id) ?: error("...")
                val updatedDerivedStockAnalytics = stockAnalysis
                    .derivedStockAnalytics
                    ?.copy(currentPrice = iexService.price(stockAnalysis.ticker).orZero())
                stockAnalysisService.saveStockAnalysis(stockAnalysis.copy(derivedStockAnalytics = updatedDerivedStockAnalytics))
            } catch (e: Exception) {
                // TODO report the failure somewhere
            }
        }
    }

}