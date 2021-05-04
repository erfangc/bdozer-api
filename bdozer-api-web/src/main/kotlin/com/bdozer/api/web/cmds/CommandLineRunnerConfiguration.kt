package com.bdozer.api.web.cmds

import bdozer.api.common.stockanalysis.EvaluateModelRequest
import bdozer.api.common.stockanalysis.StockAnalysis2
import com.bdozer.api.web.factbase.modelbuilder.ModelBuilderFactory
import com.bdozer.api.web.filingentity.FilingEntityManager
import com.bdozer.api.web.stockanalysis.StockAnalysisService
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class CommandLineRunnerConfiguration {

    private val log = LoggerFactory.getLogger(CommandLineRunnerConfiguration::class.java)

    @Bean
    fun commandLineRunner(
        stockAnalysisService: StockAnalysisService,
        modelBuilderFactory: ModelBuilderFactory,
        filingEntityManager: FilingEntityManager,
    ): CommandLineRunner {

        val outputTxt = File("output.txt")
        val rows = outputTxt
            .readLines()
            .map {
                val split = it.split("\t")
                val cik = split[0]
                val adsh = split[1]
                val formType = split[2]
                val period = split[3]
                Row(cik, adsh, formType, period)
            }
            .groupBy { it.cik }
            .mapValues { entry ->
                entry
                    .value
                    .filter { row -> row.formType == "10-K" }
                    .maxByOrNull { row -> row.period }
            }
            .values
            .filterNotNull()

        return CommandLineRunner {
            val errors = mutableListOf<Error>()
            for (row in rows) {
                val cik = row.cik
                val adsh = row.adsh
                log.info("Processing cik=$cik adsh=$adsh")
                val filingEntity = try {
                    filingEntityManager.getFilingEntity(cik) ?: filingEntityManager.createFilingEntity(cik)
                } catch (e: Exception) {
                    continue
                }

                try {
                    val model = modelBuilderFactory.bestEffortModel(
                        cik = cik,
                        adsh = adsh
                    )
                    val request = EvaluateModelRequest(model)
                    val resp = stockAnalysisService.evaluateStockAnalysis(request)
                    stockAnalysisService.saveStockAnalysis(
                        StockAnalysis2(
                            _id = "automated_$cik",
                            cells = resp.cells,
                            ticker = filingEntity.tradingSymbol,
                            name = filingEntity.name,
                            derivedStockAnalytics = resp.derivedStockAnalytics,
                            model = resp.model.copy(adsh = adsh),
                            tags = listOf("RS3000", "Automated")
                        )
                    )
                } catch (e: Exception) {
                    errors.add(
                        Error(
                            ticker = filingEntity.tradingSymbol ?: "",
                            cik = cik,
                            message = e.message ?: "No error message"
                        )
                    )
                }
            }

            val printWriter = File("errors.txt").printWriter()
            errors.forEach { error ->
                printWriter.println("${error.ticker}\t${error.cik}\t${error.message}")
            }
        }
    }

}