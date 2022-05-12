package com.bdozer.api.web.stockanalysis

import com.bdozer.api.models.dataclasses.Model
import com.bdozer.api.stockanalysis.models.FindStockAnalysisResponse
import com.bdozer.api.stockanalysis.models.StockAnalysis2
import com.bdozer.api.web.stockanalysis.support.zacks.ZacksDerivedTag
import org.springframework.web.bind.annotation.*

@RequestMapping("api/stock-analyzer/stock-analyses")
@CrossOrigin
@RestController
class StockAnalysisController(
    private val stockAnalysisService: StockAnalysisService,
) {

    @PostMapping("evaluate")
    fun evaluateStockAnalysis(
        @RequestBody model: Model,
        @RequestParam(required = false) saveAs: String? = null,
        @RequestParam(required = false) published: Boolean = false,
        @RequestParam(required = false) tags: List<String> = emptyList(),
    ): StockAnalysis2 {
        return stockAnalysisService.evaluateStockAnalysis(
            model = model,
            saveAs = saveAs,
            published = published,
            tags = tags,
        )
    }

    @DeleteMapping("{id}")
    fun deleteStockAnalysis(@PathVariable id: String) {
        stockAnalysisService.deleteStockAnalysis(id)
    }

    @GetMapping("{id}")
    fun getStockAnalysis(@PathVariable id: String): StockAnalysis2? {
        return stockAnalysisService.getStockAnalysis(id)
    }

    @PutMapping("{id}")
    fun saveStockAnalysis(
        @PathVariable id: String, 
        @RequestBody stockAnalysis2: StockAnalysis2
    ) {
        return stockAnalysisService.saveStockAnalysis(stockAnalysis2)
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
        @RequestParam(required = false) zacksDerivedTags: List<ZacksDerivedTag>? = null,
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
            zacksDerivedTags = zacksDerivedTags,
            sort = sort,
        )
    }

}