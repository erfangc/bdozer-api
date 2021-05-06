package com.bdozer.api.web.stockanalysis.cron

import com.bdozer.api.stockanalysis.iex.IEXService
import com.bdozer.api.stockanalysis.StockAnalysisService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class StockAnalysisCronJobs(
    private val stockAnalysisService: StockAnalysisService,
    private val iexService: IEXService
) {

    private val log = LoggerFactory.getLogger(StockAnalysisCronJobs::class.java)

    /**
     * Run price updates for all analyses
     * at 4pm each Mon day through Friday
     */
    @Scheduled(cron = "0 0 16 * * MON-FRI")
    fun updatePrices() {
        val stockAnalyses = stockAnalysisService.findStockAnalyses(limit = 5000).stockAnalyses
        val tickers = stockAnalyses.mapNotNull { it.ticker }.distinct()
        val prices = iexService.prices(tickers = tickers)
        stockAnalyses.forEach {
            val id = it._id
            try {
                val stockAnalysis = stockAnalysisService.getStockAnalysis(id) ?: error("stock analysis not found")
                val currentPrice = prices[it.ticker] ?: error("no prices found for ticker=${it.ticker}")
                val updatedDerivedStockAnalytics = stockAnalysis.derivedStockAnalytics?.copy(currentPrice = currentPrice)
                stockAnalysisService.saveStockAnalysis(stockAnalysis.copy(derivedStockAnalytics = updatedDerivedStockAnalytics))
                log.info("Updated stock analysis id=$id ticker=${stockAnalysis.ticker}")
            } catch (e: Exception) {
                log.error("Unable to update stock analysis id=$id, error: ${e.message}")
            }
        }
    }

}