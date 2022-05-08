package com.bdozer.api.web.stockanalysis.cron

import com.bdozer.api.stockanalysis.StockAnalysisService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class StockAnalysisCronJobs(
    private val stockAnalysisService: StockAnalysisService,
) {

    private val log = LoggerFactory.getLogger(StockAnalysisCronJobs::class.java)

    /**
     * Run price updates for all analyses
     * at 4pm each Mon day through Friday
     */
    @Scheduled(cron = "0 0 16 * * MON-FRI")
    fun updatePrices() {
        val stockAnalyses = stockAnalysisService.findStockAnalyses(limit = 5000, published = true).stockAnalyses
        log.info("Updating rerunning ${stockAnalyses.size} stock analyses for price updates")
        stockAnalyses.forEach {
            val stockAnalysisId = it._id!!
            try {
                val stockAnalysis = stockAnalysisService.getStockAnalysis(stockAnalysisId)
                log.info("Updating stock analysis id=$stockAnalysisId ticker=${stockAnalysis.ticker}")
                stockAnalysisService.refreshStockAnalysis(stockAnalysisId = stockAnalysisId, save = true)
            } catch (e: Exception) {
                log.error("Unable to update stock analysis id=$stockAnalysisId, error: ${e.message}")
            }
        }
        log.info("Price updates and derived analytics recompute complete for ${stockAnalyses.size} stock analyses")
    }

}