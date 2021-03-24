package com.starburst.starburst.modelbuilder

import com.starburst.starburst.edgar.explorer.EdgarExplorer
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.provider.FilingProviderFactory
import com.starburst.starburst.filingentity.FilingEntityManager
import com.starburst.starburst.modelbuilder.common.ModelResult
import com.starburst.starburst.modelbuilder.templates.Recovery
import com.starburst.starburst.zacks.se.ZacksEstimatesService
import org.springframework.stereotype.Service

@Service
class ModelBuilderFactory(
    private val factBase: FactBase,
    private val filingProviderFactory: FilingProviderFactory,
    private val zacksEstimatesService: ZacksEstimatesService,
    private val edgarExplorer: EdgarExplorer,
    private val filingEntityManager: FilingEntityManager,
) {

    fun createModel(cik: String): ModelResult {
        val cik = cik.padStart(10, '0')
        val edgarFilingMetadata =
            edgarExplorer.latestFiscalFiling(cik) ?: error("Unable to find latest fiscal filing for $cik")
        val adsh = edgarFilingMetadata.adsh
        val modelBuilder = recovery(cik, adsh)
        return modelBuilder.buildModel()
    }

    private fun recovery(cik: String, adsh: String) = Recovery(
        filingProvider = filingProviderFactory.createFilingProvider(cik, adsh),
        factBase = factBase,
        filingEntityManager = filingEntityManager,
        zacksEstimatesService = zacksEstimatesService,
    )
}