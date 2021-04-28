package com.bdozer.stockanalysis.excel

import org.springframework.http.HttpEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("public/stock-analyses")
class StockAnalysisExcelDownloaderController(
    private val stockAnalysisExcelDownloader: StockAnalysisExcelDownloader
) {

    @GetMapping("{id}/excel-download")
    fun download(@PathVariable id: String): HttpEntity<ByteArray> {
        return stockAnalysisExcelDownloader.download(id)
    }

}