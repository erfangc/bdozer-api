package com.bdozer.stockanalyzer.itemgenerator

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.bdozer.edgar.explorer.EdgarExplorer
import com.bdozer.edgar.factbase.FilingProviderFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import java.io.File

internal class ItemGeneratorTest {

    init {
        listOf("org.apache.http").forEach {
            val logger = LoggerFactory.getLogger(it) as Logger
            logger.isAdditive = false
            logger.level = Level.ERROR
        }
    }

    @Test
    fun run() {
        val http = HttpClientBuilder.create().build()
        val factory = FilingProviderFactory(http)
        val objectMapper = jacksonObjectMapper().findAndRegisterModules()
        val edgarExplorer = EdgarExplorer(http = http, objectMapper = objectMapper)

        val lines = ClassPathResource("snp500.txt").inputStream.bufferedReader().readLines()
        val writer = File("output.txt").printWriter()

        lines.forEach { line ->

            val parts = line.split("\t")
            val ticker = parts[0]
            val cik = parts[7]

            try {
                val adsh = edgarExplorer.latestFiscalFiling(cik)?.adsh ?: error("no adsh found")
                try {
                    val filingProvider = factory.createFilingProvider(cik = cik, adsh = adsh)
                    val generator = ItemGenerator(filingProvider)
                    val generateItemsResponse = generator.generateItems()
                    println("$ticker done")
                    writer.println("$ticker\t$cik\t$adsh\tOk")
                } catch (e: Exception) {
                    println("$ticker done")
                    writer.println("$ticker\t$cik\t$adsh\t${e.message}")
                }
            } catch (e: Exception) {
                println("$ticker done")
                writer.println("$ticker\t$cik\t\t${e.message}")
            }
        }
        writer.close()
    }
}