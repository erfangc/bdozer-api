package com.bdozer.stockanalyzer

import com.bdozer.stockanalyzer.dataclasses.FindStockAnalysisResponse
import com.bdozer.stockanalyzer.dataclasses.StockAnalysis2
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("public/published-stock-analyses")
class PublishedStockAnalysisController(
    private val stockAnalysisService: StockAnalysisService
) {

    @GetMapping("{id}")
    fun getPublishedStockAnalysis(@PathVariable id: String): StockAnalysis2? {
        return stockAnalysisService.get(id)
    }

    @GetMapping
    fun findPublishedStockAnalyses(
        @RequestParam(required = false) userId: String? = null,
        @RequestParam(required = false) cik: String? = null,
        @RequestParam(required = false) ticker: String? = null,
        @RequestParam(required = false) skip: Int? = null,
        @RequestParam(required = false) limit: Int? = null,
        @RequestParam(required = false) term: String? = null,
    ): FindStockAnalysisResponse {
        return stockAnalysisService.find(
            userId = userId,
            cik = cik,
            ticker = ticker,
            published = true,
            skip = skip,
            limit = limit,
            term = term,
        )
    }

}