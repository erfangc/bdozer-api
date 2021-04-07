package com.starburst.starburst.stockanalyzer

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.*

@Deprecated("do not use")
@CrossOrigin
@RequestMapping("public/stock-analysis-excel-downloader")
@RestController
class StockAnalysisExcelDownloader(
    private val stockAnalyzerFactory: StockAnalyzerFactory,
) {

    @GetMapping("{cik}")
    fun exportExcel(@PathVariable cik: String): HttpEntity<ByteArray> {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.CONTENT_TYPE, "application/vnd.ms-excel")
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$cik.xlsx")
        val excel = stockAnalyzerFactory.excelModel(cik)
        return HttpEntity(excel, headers)
    }

}