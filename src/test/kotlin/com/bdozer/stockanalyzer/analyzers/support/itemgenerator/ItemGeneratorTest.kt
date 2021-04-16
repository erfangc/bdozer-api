package com.bdozer.stockanalyzer.analyzers.support.itemgenerator

import com.bdozer.edgar.factbase.FilingProviderFactory
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.jupiter.api.Test

internal class ItemGeneratorTest {

    @Test
    fun run() {
        val http = HttpClientBuilder.create().build()
        val factory = FilingProviderFactory(http)
        val filingProvider = factory.createFilingProvider(cik = "0000815097", adsh = "000081509721000027")
        val generator = ItemGenerator(filingProvider)
        val resp = generator.generateItems()
        println(resp)
    }



}