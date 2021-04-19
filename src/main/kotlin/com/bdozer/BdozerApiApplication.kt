package com.bdozer

import com.bdozer.sec.factbase.modelbuilder.ModelBuilderFactoryController
import com.bdozer.sec.factbase.modelbuilder.issues.IssuesController
import com.bdozer.stockanalysis.dataclasses.StockAnalysis2
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean

@EnableCaching
@SpringBootApplication
class BdozerApiApplication {
    @Bean
    fun commandLineRunner(
        objectMapper: ObjectMapper,
        issuesController: IssuesController,
        modelBuilderFactoryController: ModelBuilderFactoryController,
    ): CommandLineRunner {
        return CommandLineRunner {
            val model = modelBuilderFactoryController.bestEffortModel(
                cik = "1467858",
                adsh = "000146785821000037",
            )
            val stockAnalysis = StockAnalysis2(
                cik = model.cik,
                ticker = model.ticker,
                name = model.name,
                model = model,
            )
            println("----- Generating Issues ------")
            val issues = issuesController.generateIssues(stockAnalysis)
            issues.forEach { issue ->
                println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(issue))
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<BdozerApiApplication>(*args)
}

