package com.starburst.starburst.edgar.factbase.support

import com.starburst.starburst.edgar.explorer.EdgarExplorer
import com.starburst.starburst.edgar.factbase.ingestor.FilingIngestor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FactsBootstrapper(
    private val filingIngestor: FilingIngestor,
    private val edgarExplorer: EdgarExplorer,
) {

    private val log = LoggerFactory.getLogger(FactsBootstrapper::class.java)

    fun bootstrapFacts(cik: String) {
        /*
        Find the most recent 4 10-Qs and the most recent 10K and ingest those
         */
        val hits = edgarExplorer.searchFilings(cik)
        val tenKs = hits.filter { it.form == "10-K" }.sortedByDescending { it.period_ending }
        val tenQs = hits.filter { it.form == "10-Q" }.sortedByDescending { it.period_ending }

        log.info("Bootstrapping facts cik=$cik, tenKs.size=${tenKs.size}, tenQs.size=${tenQs.size}")

        val paddedCik = cik.padStart(10, '0')

        val tenKResults = tenKs.mapNotNull { recent10K ->
            try {
                filingIngestor.ingestFiling(cik = paddedCik, adsh = recent10K.adsh)
            } catch (e: Exception) {
                log.error("Unable to finish ingesting $paddedCik, adsh=${recent10K.adsh}", e)
                null
            }
        }

        val tenQResults = tenQs.mapNotNull { recent10Q ->
            try {
                filingIngestor.ingestFiling(cik = paddedCik, adsh = recent10Q.adsh)
            } catch (e: Exception) {
                log.error("Unable to finish ingesting $paddedCik, adsh=${recent10Q.adsh}", e)
                null
            }
        }

        /*
        for every 10-K that has at least 3 10Qs backing it, back-fill Q4 data
         */
        for (tenK in tenKResults) {
            val year = tenK.documentFiscalYearFocus
            val numTenQsInYear =
                tenQResults.filter { tenQResult -> tenQResult.documentFiscalYearFocus == year }.size
            if (numTenQsInYear >= 3) {
                filingIngestor.ingestQ4Facts(paddedCik, year)
            }
        }

    }
}
