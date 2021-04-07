package com.starburst.starburst.stockanalyzer.staging

import com.starburst.starburst.stockanalyzer.staging.dataclasses.StockAnalysis2
import org.litote.kmongo.findOneById
import org.springframework.web.bind.annotation.*

@RequestMapping("api/stock-analyzer/stock-analyses")
@CrossOrigin
@RestController
class StockAnalysisCRUDController(private val stockAnalysisCRUDService: StockAnalysisCRUDService) {

    @PostMapping
    fun save(@RequestBody analysis: StockAnalysis2) {
        stockAnalysisCRUDService.save(analysis)
    }

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: String) {
        stockAnalysisCRUDService.delete(id)
    }

    @GetMapping("{id}")
    fun get(@PathVariable id: String): StockAnalysis2? {
        return stockAnalysisCRUDService.get(id)
    }

    @GetMapping
    fun find(
        @RequestParam(required = false) userId: String? = null,
        @RequestParam(required = false) cik: String? = null,
        @RequestParam(required = false) ticker: String? = null,
    ): List<StockAnalysis2> {
        return stockAnalysisCRUDService.find(userId, cik, ticker)
    }

}