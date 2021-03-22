package com.starburst.starburst.modelbuilder

import com.starburst.starburst.edgar.explorer.EdgarExplorer
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.provider.FilingProviderFactory
import com.starburst.starburst.models.dataclasses.Model
import org.springframework.stereotype.Service

@Service
class ModelBuilderFactory(
    private val factBase: FactBase,
    private val filingProviderFactory: FilingProviderFactory,
    private val edgarExplorer: EdgarExplorer,
) {
    fun createModel(cik: String): Model {
        val cik = cik.padStart(10, '0')
        val metadata = edgarExplorer.latestFiscalFiling(cik) ?: error("Unable to find latest fiscal filing for $cik")
        val adsh = metadata.adsh
        val modelBuilder = ModelBuilder(
            filingProvider = filingProviderFactory.createFilingProvider(cik, adsh),
            factBase
        )
        return modelBuilder.buildModel()
    }
}