package com.starburst.starburst.stockanalyzer.staging

import com.starburst.starburst.stockanalyzer.staging.dataclasses.StockAnalysis2
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("public/published-stock-analyses")
class StockAnalysisPublicationController(
    private val stockAnalysisPublicationService: StockAnalysisPublicationService
) {
    @PostMapping
    fun publish(@RequestBody stockAnalysis: StockAnalysis2) {
        stockAnalysisPublicationService.publish(stockAnalysis)
    }

    @GetMapping("{id}")
    fun get(@PathVariable id: String): StockAnalysis2? {
        return stockAnalysisPublicationService.get(id)
    }

    @DeleteMapping("{id}")
    fun unpublish(@PathVariable id: String) {
        stockAnalysisPublicationService.unpublish(id)
    }

    @GetMapping
    fun find(
        @RequestParam(required = false) skip: Int? = null,
        @RequestParam(required = false) limit: Int? = null,
    ): List<StockAnalysis2> {
        return stockAnalysisPublicationService.find(skip, limit)
    }

}