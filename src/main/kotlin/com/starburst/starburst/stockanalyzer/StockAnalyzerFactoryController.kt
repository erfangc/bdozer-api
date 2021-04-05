package com.starburst.starburst.stockanalyzer

import com.starburst.starburst.stockanalyzer.dataclasses.StockAnalysis
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("public/model-builder-factory")
class StockAnalyzerFactoryController(private val stockAnalyzerFactory: StockAnalyzerFactory) {

    @PostMapping
    fun saveAnalysis(@RequestBody stockAnalysis: StockAnalysis) {
        stockAnalyzerFactory.save(stockAnalysis)
    }

    @GetMapping("{cik}")
    fun analyze(@PathVariable cik: String): StockAnalysis {
        return stockAnalyzerFactory.analyze(cik)
    }

    @GetMapping
    fun getAnalyses(): List<StockAnalysis> {
        return stockAnalyzerFactory.getAnalyses()
    }

    @GetMapping("{cik}/analysis")
    fun getAnalysis(@PathVariable cik: String): StockAnalysis {
        return stockAnalyzerFactory.getAnalysis(cik)
    }
}