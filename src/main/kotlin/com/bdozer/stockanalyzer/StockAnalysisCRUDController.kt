package com.bdozer.stockanalyzer

import com.bdozer.stockanalyzer.dataclasses.FindStockAnalysisResponse
import com.bdozer.stockanalyzer.dataclasses.StockAnalysis2
import org.litote.kmongo.findOneById
import org.springframework.web.bind.annotation.*

@RequestMapping("api/stock-analyzer/stock-analyses")
@CrossOrigin
@RestController
class StockAnalysisCRUDController(private val stockAnalysisCRUDService: StockAnalysisCRUDService) {

    @PostMapping
    fun saveStockAnalysis(@RequestBody analysis: StockAnalysis2) {
        stockAnalysisCRUDService.save(analysis)
    }

    @DeleteMapping("{id}")
    fun deleteStockAnalysis(@PathVariable id: String) {
        stockAnalysisCRUDService.delete(id)
    }

    @GetMapping("{id}")
    fun getStockAnalysis(@PathVariable id: String): StockAnalysis2? {
        return stockAnalysisCRUDService.get(id)
    }

    @GetMapping
    fun findStockAnalyses(
        @RequestParam(required = false) userId: String? = null,
        @RequestParam(required = false) cik: String? = null,
        @RequestParam(required = false) ticker: String? = null,
        @RequestParam(required = false) skip: Int? = null,
        @RequestParam(required = false) limit: Int? = null,
        @RequestParam(required = false) term: String? = null,
    ): FindStockAnalysisResponse {
        return stockAnalysisCRUDService.find(userId, cik, ticker, skip, limit, term)
    }

}