package com.bdozer.api.web.stockanalysis

import com.bdozer.api.stockanalysis.SortDirection
import com.bdozer.api.stockanalysis.dataclasses.FindStockAnalysisResponse
import com.bdozer.api.stockanalysis.dataclasses.StockAnalysis2
import com.bdozer.api.stockanalysis.StockAnalysisService
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("public/published-stock-analyses")
class PublishedStockAnalysisController(
    private val stockAnalysisService: StockAnalysisService
) {

    @GetMapping("{id}")
    fun getPublishedStockAnalysis(@PathVariable id: String): StockAnalysis2? {
        return stockAnalysisService.getStockAnalysis(id)
    }

    @GetMapping("top4")
    fun top4StockAnalyses(): FindStockAnalysisResponse {
        return stockAnalysisService.top4StockAnalyses()
    }

    @GetMapping
    fun findPublishedStockAnalyses(
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
            published = true,
            skip = skip,
            limit = limit,
            term = term,
            tags = tags,
            sort = sort,
        )
    }

}