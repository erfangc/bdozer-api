package com.bdozer.api.web.stockanalysis.excel

import com.bdozer.api.models.CellGenerator
import com.bdozer.api.stockanalysis.StockAnalysisService
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service

@Service
class StockAnalysisExcelDownloader(
    private val stockAnalysisService: StockAnalysisService,
) {

    fun download(id: String): HttpEntity<ByteArray> {
        val stockAnalysis = stockAnalysisService.getStockAnalysis(id) ?: error("...")
        val headers = HttpHeaders()
        headers.set(HttpHeaders.CONTENT_TYPE, "application/vnd.ms-excel")
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=${stockAnalysis.cik}.xlsx")
        val byteArray = CellGenerator.exportToXls(stockAnalysis.model, stockAnalysis.cells)
        return HttpEntity(byteArray, headers)
    }

}