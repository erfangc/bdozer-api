package com.bdozer.api.web.stockanalysis

import com.bdozer.api.web.stockanalysis.dataclasses.EvaluateModelRequest
import com.bdozer.api.web.stockanalysis.dataclasses.EvaluateModelResponse
import com.bdozer.api.web.stockanalysis.dataclasses.FindStockAnalysisResponse
import com.bdozer.api.web.stockanalysis.dataclasses.StockAnalysis2
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.*

@RequestMapping("api/stock-analyzer/stock-analyses")
@CrossOrigin
@RestController
class StockAnalysisController(private val stockAnalysisService: StockAnalysisService) {

    @Operation(
        description = """
        This API evaluates a model you've assembled and return a stock analysis object 
        
        The passed in Model represents high level relationship between the various financial statement items of underlying a stock 
        Calling this method evaluates those relationships and turn them into real numbers
        
        This API does not persist (save) the stock analysis. Please call the stock analysis service API to save the analysis
        
        This is a stateless calculator
        """
    )
    @PostMapping("evaluate")
    fun evaluateStockAnalysis(@RequestBody request: EvaluateModelRequest): EvaluateModelResponse {
        return stockAnalysisService.evaluateStockAnalysis(request)
    }

    @Operation(
        description = """
        This API refreshes an existing stock analysis and re-evaluate
        the model attached to it to produce renewed outputs. Call this API 
        when you are in possession of a previously run stock analysis
        
        The returned refreshed stock analysis preserve all the metadata, model overrides
        of the original analysis
        
        This API does not persist (save) the new analysis. This API is a stateless calculator
        """,
        summary = """Refresh a stock analysis by rerunning the model"""
    )
    @PostMapping("refresh")
    fun refreshStockAnalysis(@RequestBody stockAnalysis: StockAnalysis2): StockAnalysis2 {
        return stockAnalysisService.refreshStockAnalysis(stockAnalysis)
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
        )
    }

}