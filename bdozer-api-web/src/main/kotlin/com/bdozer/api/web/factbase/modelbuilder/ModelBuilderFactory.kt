package com.bdozer.api.web.factbase.modelbuilder

import com.bdozer.api.factbase.core.SECFilingFactory
import bdozer.api.common.model.Model
import org.springframework.stereotype.Service

@Service
class ModelBuilderFactory(
    private val secFilingFactory: SECFilingFactory,
) {
    fun bestEffortModel(cik: String, adsh: String): Model {
        val modelBuilder = ModelBuilder(secFiling = secFilingFactory.createSECFiling(cik, adsh))
        return modelBuilder.bestEffortModel()
    }
}