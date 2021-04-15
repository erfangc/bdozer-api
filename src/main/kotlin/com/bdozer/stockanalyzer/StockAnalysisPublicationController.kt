package com.bdozer.stockanalyzer

import com.bdozer.stockanalyzer.dataclasses.StockAnalysis2
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("public/published-stock-analyses")
class StockAnalysisPublicationController(
    private val stockAnalysisPublicationService: StockAnalysisPublicationService
) {
    @PostMapping
    fun publishStockAnalysis(@RequestBody stockAnalysis: StockAnalysis2) {
        stockAnalysisPublicationService.publish(stockAnalysis)
    }

    @GetMapping("{id}")
    fun getPublishedStockAnalysis(@PathVariable id: String): StockAnalysis2? {
        return stockAnalysisPublicationService.get(id)
    }

    @DeleteMapping("{id}")
    fun unpublishStockAnalysis(@PathVariable id: String) {
        stockAnalysisPublicationService.unpublish(id)
    }

    @GetMapping
    fun findPublishedStockAnalyses(
        @RequestParam(required = false) skip: Int? = null,
        @RequestParam(required = false) limit: Int? = null,
    ): List<StockAnalysis2> {
        return stockAnalysisPublicationService.find(skip, limit)
    }

}