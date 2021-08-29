package com.bdozer.api.web.stockanalysis

import com.bdozer.api.stockanalysis.SortDirection
import com.bdozer.api.stockanalysis.StockAnalysisService
import com.bdozer.api.stockanalysis.dataclasses.EvaluateModelRequest
import com.bdozer.api.stockanalysis.dataclasses.FindStockAnalysisResponse
import com.bdozer.api.stockanalysis.dataclasses.StockAnalysis2
import org.springframework.web.bind.annotation.*

@RequestMapping("api/stock-analyzer/stock-analyses")
@CrossOrigin
@RestController
class StockAnalysisController(
    private val stockAnalysisService: StockAnalysisService,
) {

    @PostMapping("evaluate")
    fun evaluateStockAnalysis(
        @RequestBody request: EvaluateModelRequest,
        @RequestParam(required = false) saveAs: String? = null
    ): StockAnalysis2 {
        return stockAnalysisService.evaluateStockAnalysis(request = request, saveAs = saveAs)
    }

    @PostMapping("{id}/refresh")
    fun refreshStockAnalysis(
        @PathVariable id: String,
        @RequestParam(required = false) save: Boolean? = null
    ): StockAnalysis2 {
        return stockAnalysisService.refreshStockAnalysis(stockAnalysisId = id, save = save)
    }

    @PostMapping
    fun saveStockAnalysis(@RequestBody analysis: StockAnalysis2) {
        stockAnalysisService.saveStockAnalysis(analysis)
    }

    @DeleteMapping("{id}")
    fun deleteStockAnalysis(@PathVariable id: String) {
        stockAnalysisService.deleteStockAnalysis(id)
    }

    @GetMapping("{id}")
    fun getStockAnalysis(@PathVariable id: String): StockAnalysis2? {
        return stockAnalysisService.getStockAnalysis(id)
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
        @RequestParam(required = false) tags: List<String>? = null,
        @RequestParam(required = false) sort: SortDirection? = null,
    ): FindStockAnalysisResponse {
        return stockAnalysisService.findStockAnalyses(
            userId = userId,
            cik = cik,
            ticker = ticker,
            skip = skip,
            limit = limit,
            term = term,
            published = published,
            tags = tags,
            sort = sort,
        )
    }

}