package com.starburst.starburst.edgar.bootstrapper

import com.starburst.starburst.edgar.explorer.EdgarExplorer
import com.starburst.starburst.edgar.factbase.ingestor.FilingIngestor
import com.starburst.starburst.edgar.factbase.modelbuilder.ModelBuilder
import com.starburst.starburst.models.Model
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class FilingEntityBootstrapper(
    private val filingIngestor: FilingIngestor,
    private val edgarExplorer: EdgarExplorer,
    private val modelBuilder: ModelBuilder
) {

    private val log = LoggerFactory.getLogger(FilingEntityBootstrapper::class.java)

    fun buildModelWithLatest10K(cik: String): Model {
        val adsh = edgarExplorer
            .searchFilings(cik)
            .sortedByDescending { it.period_ending }
            .find { it.form == "10-K" }
            ?.adsh ?: error("no 10-K filings found for $cik")
        return modelBuilder.buildModelForFiling(cik, adsh)
    }

    fun bootstrapFilingEntity(cik: String) {
        //
        // Find the most recent 4 10-Qs and the most recent 10K and ingest those
        //
        val hits = edgarExplorer.searchFilings(cik)
        val tenKs = hits.filter { it.form == "10-K" }.sortedByDescending { it.period_ending }
        val tenQs = hits.filter { it.form == "10-Q" }.sortedByDescending { it.period_ending }
        val recent10Ks = tenKs.first()
        val recent10Qs = tenQs.subList(0, 4)
        try {
            filingIngestor.ingestFiling(
                cik = cik,
                adsh = recent10Ks.adsh
            )
            for (recent10Q in recent10Qs) {
                filingIngestor.ingestFiling(
                    cik = cik,
                    adsh = recent10Q.adsh
                )
            }
            log.info("Completed bootstrapping cik=$cik")
        } catch (e: Exception) {
            log.error("Unable to complete bootstrapping cik=$cik", e)
        }
    }
}