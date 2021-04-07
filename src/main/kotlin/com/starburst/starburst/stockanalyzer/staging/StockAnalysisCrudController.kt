package com.starburst.starburst.stockanalyzer.staging

import com.starburst.starburst.stockanalyzer.staging.dataclasses.StockAnalysis2
import org.springframework.web.bind.annotation.*

@RequestMapping("api/stock-analyzer/stock-analyses")
@CrossOrigin
@RestController
class StockAnalysisCrudController(private val stockAnalysisCrudService: StockAnalysisCrudService) {

    @PostMapping
    fun save(@RequestBody analysis: StockAnalysis2) {
        stockAnalysisCrudService.save(analysis)
    }

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: String) {
        stockAnalysisCrudService.delete(id)
    }

    @GetMapping
    fun find(
        @RequestParam userId: String? = null,
        @RequestParam cik: String? = null,
        @RequestParam ticker: String? = null,
    ): List<StockAnalysis2> {
        return stockAnalysisCrudService.find(userId, cik, ticker)
    }

}