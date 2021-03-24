package com.starburst.starburst.modelbuilder

import com.starburst.starburst.modelbuilder.common.StockAnalysis
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("public/model-builder-factory")
class StockAnalyzerFactoryController(private val stockAnalyzerFactory: StockAnalyzerFactory) {
    @GetMapping("{cik}")
    fun analyze(
        @PathVariable cik: String,
        @RequestParam(required = false) save: Boolean? = null
    ): StockAnalysis {
        return stockAnalyzerFactory.analyze(cik, save)
    }

    @GetMapping
    fun getAnalyses(): List<StockAnalysis> {
        return stockAnalyzerFactory.getAnalyses()
    }
}