package com.bdozer.api.factbase.modelbuilder

import com.bdozer.api.factbase.core.SECFilingFactory
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.jupiter.api.Test

internal class ModelBuilderTest {

    val secFilingFactory = SECFilingFactory(http = HttpClientBuilder.create().build())

    @Test
    internal fun buildModel() {
        val secFiling = secFilingFactory.createSECFiling(cik = "6201", adsh = "000000620121000014")
        val modelBuilder = ModelBuilder(secFiling = secFiling)
        val model = modelBuilder.bestEffortModel()
        model.incomeStatementItems
    }
}