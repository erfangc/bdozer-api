package com.starburst.starburst.edgar.factbase

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.starburst.starburst.AppConfiguration
import com.starburst.starburst.edgar.explorer.EdgarExplorer
import com.starburst.starburst.edgar.provider.FilingProviderFactory
import org.junit.jupiter.api.Test

internal class FactComponentFinderTest {
    @Test
    fun test() {
        val cfg = AppConfiguration()
        val client = cfg.mongoClient()
        val http = cfg.httpClient()
        val objectMapper = jacksonObjectMapper().findAndRegisterModules()

        val obj = FactComponentFinder(
            mongoDatabase = cfg.mongoDatabase(client),
            edgarExplorer = EdgarExplorer(http, objectMapper),
            filingProviderFactory = FilingProviderFactory(http)
        )

        val components = obj.components(cik = "0001467623", conceptId = "us-gaap_OperatingExpenses")
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(components))
    }
}