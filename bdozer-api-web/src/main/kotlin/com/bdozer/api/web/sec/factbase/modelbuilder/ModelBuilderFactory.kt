package com.bdozer.api.web.sec.factbase.modelbuilder

import com.bdozer.api.web.models.dataclasses.Model
import com.bdozer.api.web.sec.factbase.filing.SECFilingFactory
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