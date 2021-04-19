package com.bdozer.sec.factbase.modelbuilder

import com.bdozer.sec.factbase.filing.SECFilingFactory
import com.bdozer.models.dataclasses.Model
import org.springframework.stereotype.Service

@Service
class ModelBuilderFactory(
    private val secFilingFactory: SECFilingFactory
) {
    fun bestEffortModel(cik: String, adsh: String): Model {
        val modelBuilder = ModelBuilder(secFiling = secFilingFactory.createSECFiling(cik, adsh))
        return modelBuilder.bestEffortModel()
    }
}