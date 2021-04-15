package com.bdozer.stockanalyzer

import com.bdozer.stockanalyzer.dataclasses.FindStockAnalysisResponse
import com.bdozer.stockanalyzer.dataclasses.StockAnalysis2
import org.springframework.web.bind.annotation.*

@RequestMapping("api/stock-analyzer/stock-analyses")
@CrossOrigin
@RestController
class StockAnalysisController(private val stockAnalysisService: StockAnalysisService) {

    @PostMapping
    fun saveStockAnalysis(@RequestBody analysis: StockAnalysis2) {
        stockAnalysisService.save(analysis)
    }

    @DeleteMapping("{id}")
    fun deleteStockAnalysis(@PathVariable id: String) {
        stockAnalysisService.delete(id)
    }

    @GetMapping("{id}")
    fun getStockAnalysis(@PathVariable id: String): StockAnalysis2? {
        return stockAnalysisService.get(id)
    }

    @PostMapping("{id}/publish")
    fun publish(@PathVariable id: String): StockAnalysis2 {
        return stockAnalysisService.publish(id)
    }

    @PostMapping("{id}/unpublish")
    fun unpublish(@PathVariable id: String): StockAnalysis2 {
        return stockAnalysisService.unpublish(id)
    }

    @GetMapping
    fun findStockAnalyses(
        @RequestParam(required = false) published: Boolean? = null,
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
            skip = skip,
            limit = limit,
            term = term,
            published = published
        )
    }

}