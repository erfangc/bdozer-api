package com.starburst.starburst.stockanalyzer

import org.springframework.http.HttpEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/stock-analyzer/workflow")
class StockAnalysisWorkflowDownloadController(
    private val stockAnalysisWorkflowService: StockAnalysisWorkflowService
) {

    @GetMapping("{id}/download")
    fun download(@PathVariable id: String): HttpEntity<ByteArray> {
        return stockAnalysisWorkflowService.download(id)
    }

}