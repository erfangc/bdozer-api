package com.bdozer.api.factbase.modelbuilder

import com.bdozer.api.factbase.core.SECFilingFactory
import com.bdozer.api.models.dataclasses.Model

class ModelBuilderFactory(
    private val secFilingFactory: SECFilingFactory,
) {
    fun bestEffortModel(cik: String, adsh: String): Model {
        val modelBuilder = ModelBuilder(secFiling = secFilingFactory.createSECFiling(cik, adsh))
        return modelBuilder.bestEffortModel()
    }
}