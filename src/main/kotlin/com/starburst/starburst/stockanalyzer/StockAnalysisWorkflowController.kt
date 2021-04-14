package com.starburst.starburst.stockanalyzer

import com.starburst.starburst.stockanalyzer.dataclasses.StockAnalysis2
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/stock-analyzer/workflow")
class StockAnalysisWorkflowController(
    private val stockAnalysisWorkflowService: StockAnalysisWorkflowService
) {

    @GetMapping("{cik}")
    fun create(@PathVariable cik: String): StockAnalysis2 {
        return stockAnalysisWorkflowService.create(cik)
    }
    
    @PostMapping("refresh")
    fun refresh(@RequestBody stockAnalysis: StockAnalysis2): StockAnalysis2 {
        return stockAnalysisWorkflowService.refresh(stockAnalysis)
    }

}