package com.bdozer.stockanalysis.cron

import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
@RequestMapping("api/stock-analysis-cron-jobs")
class StockAnalysisCronJobsController(
    private val stockAnalysisCronJobs: StockAnalysisCronJobs
) {

    /**
     * Run price updates for all analyses
     * at 4pm each Mon day through Friday
     */
    @PostMapping("update-prices")
    fun updatePrices() {
        stockAnalysisCronJobs.updatePrices()
    }

}