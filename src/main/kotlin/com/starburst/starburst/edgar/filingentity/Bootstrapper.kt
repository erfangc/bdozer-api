package com.starburst.starburst.edgar.filingentity

import com.starburst.starburst.edgar.explorer.EdgarExplorer
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.ingestor.FilingIngestor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class Bootstrapper(
    private val filingIngestor: FilingIngestor,
    private val edgarExplorer: EdgarExplorer,
    private val factBase: FactBase,
) {

    private val log = LoggerFactory.getLogger(Bootstrapper::class.java)

    fun bootstrapFilingEntity(cik: String) {
        //
        // Find the most recent 4 10-Qs and the most recent 10K and ingest those
        //
        val hits = edgarExplorer.searchFilings(cik)
        val tenKs = hits.filter { it.form == "10-K" }.sortedByDescending { it.period_ending }
        val tenQs = hits.filter { it.form == "10-Q" }.sortedByDescending { it.period_ending }

        val recent10Ks = tenKs.subList(0, 4.coerceAtMost(tenKs.size))
        val recent10Qs = tenQs.subList(0, 4.coerceAtMost(tenQs.size))

        try {
            factBase.deleteAll(cik)

            for (recent10K in recent10Ks) {
                filingIngestor.ingestFiling(
                    cik = cik,
                    adsh = recent10K.adsh
                )
            }

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
