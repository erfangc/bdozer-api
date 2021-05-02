package com.bdozer.sec.factbase.core

import com.bdozer.sec.factbase.ingestor.FilingIngestor
import com.bdozer.sec.factbase.ingestor.cron.SecIngestorCronJobs
import org.springframework.web.bind.annotation.*
import java.util.concurrent.Executors

@RestController
@RequestMapping("api/fact-base")
@CrossOrigin
class FactBaseController(
    private val filingIngestor: FilingIngestor,
    private val secIngestorCronJobs: SecIngestorCronJobs,
) {

    private val executor = Executors.newCachedThreadPool()

    @PostMapping("latest")
    fun latest() {
        executor.execute {
            try {
                secIngestorCronJobs.processLatest()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    @PostMapping("filing-ingestor")
    fun ingestFiling(
        @RequestParam cik: String,
        @RequestParam adsh: String
    ) {
        executor.execute {
            filingIngestor.ingestFiling(cik, adsh)
        }
    }

    /**
     * Why? b/c we only have 3 10-Qs and 1 10-K
     * thus standalone Q4 info is missing
     */
    @PostMapping("filing-ingestor/q4")
    fun ingestQ4Facts(
        @RequestParam cik: String,
        @RequestParam year: Int
    ) {
        executor.execute {
            try {
                filingIngestor.ingestQ4Facts(cik, year)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
