package com.bdozer

import com.bdozer.models.dataclasses.ItemType
import com.bdozer.models.dataclasses.ManualProjection
import com.bdozer.models.dataclasses.ManualProjections
import com.bdozer.stockanalysis.StockAnalysisService
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling

@EnableCaching
@EnableScheduling
@SpringBootApplication
class BdozerApiApplication {
    @Bean
    fun commandLineRunner(stockAnalysisService: StockAnalysisService): CommandLineRunner {
        return CommandLineRunner {
//            stockAnalysisService.findStockAnalyses(limit = 1000).stockAnalyses.map { stockAnalysisProjection ->
//                try {
//                    val stockAnalysis =
//                        stockAnalysisService.getStockAnalysis(stockAnalysisProjection._id) ?: error("...")
//                    val item = stockAnalysis.model.item(stockAnalysis.model.totalRevenueConceptName) ?: error("...")
//                    val newItem = item.copy(
//                        type = ItemType.ManualProjections,
//                        manualProjections = ManualProjections(
//                            item.discrete!!.formulas.map { (period, value) ->
//                                ManualProjection(period - 2020, value.toDouble())
//                            }
//                        )
//                    )
//                    val updatedStockAnalysis = stockAnalysis.copy(
//                        model = stockAnalysis.model.copy(
//                            incomeStatementItems = stockAnalysis.model.incomeStatementItems.map {
//                                if (it.name === newItem.name) {
//                                    newItem
//                                } else {
//                                    it
//                                }
//                            }
//                        )
//                    )
//                    stockAnalysisService.saveStockAnalysis(updatedStockAnalysis)
//                    println("Updated ${stockAnalysis.ticker}")
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<BdozerApiApplication>(*args)
}

