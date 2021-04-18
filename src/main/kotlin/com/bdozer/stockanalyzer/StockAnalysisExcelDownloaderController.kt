package com.bdozer.stockanalyzer

import org.springframework.http.HttpEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/stock-analyzer/workflow")
class StockAnalysisExcelDownloaderController(
    private val stockAnalysisExcelDownloader: StockAnalysisExcelDownloader
) {

    @GetMapping("{id}/download")
    fun download(@PathVariable id: String): HttpEntity<ByteArray> {
        return stockAnalysisExcelDownloader.download(id)
    }

}