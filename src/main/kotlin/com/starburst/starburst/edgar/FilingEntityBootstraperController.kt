package com.starburst.starburst.edgar

import com.starburst.starburst.edgar.explorer.EdgarExplorer
import com.starburst.starburst.edgar.factbase.ingestor.FilingIngestor
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.util.concurrent.Executors

@RestController
@CrossOrigin
@RequestMapping("api/filing-entity-bootstrapper")
class FilingEntityBootstraperController(
    private val filingIngestor: FilingIngestor,
    private val edgarExplorer: EdgarExplorer
) {

    private val executor = Executors.newCachedThreadPool()
    private val log = LoggerFactory.getLogger(FilingEntityBootstraperController::class.java)

    @PostMapping
    fun bootstrapFilingEntity(@RequestParam cik: String) {
        //
        // Find the most recent 4 10-Qs and the most recent 10K and ingest those
        //
        val hits = edgarExplorer.searchFilings(cik)
        val tenKs = hits.filter { it.form == "10-K" }.sortedByDescending { it.period_ending }
        val tenQs = hits.filter { it.form == "10-Q" }.sortedByDescending { it.period_ending }
        val recent10Ks = tenKs.first()
        val recent10Qs = tenQs.subList(0, 4)

        executor.execute {
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
}