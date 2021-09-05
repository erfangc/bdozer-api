package com.bdozer.api.web.stockanalysisrequest

import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("public/stock-analysis-request")
class StockAnalysisRequestController(
    private val stockAnalysisRequestService: StockAnalysisRequestService
) {
    @PostMapping
    fun saveStockAnalysisRequest(@RequestParam ticker: String, @RequestParam email: String) {
        stockAnalysisRequestService.saveStockAnalysisRequest(ticker = ticker, email = email)
    }
}