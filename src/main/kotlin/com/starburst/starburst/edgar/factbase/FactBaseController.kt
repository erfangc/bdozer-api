package com.starburst.starburst.edgar.factbase

import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.ingestor.FilingIngestor
import com.starburst.starburst.edgar.factbase.ingestor.RssFilingIngestor
import org.springframework.web.bind.annotation.*
import java.util.concurrent.Executors

@RestController
@RequestMapping("api/fact-base")
@CrossOrigin
class FactBaseController(
    private val factBase: FactBase,
    private val filingIngestor: FilingIngestor,
    private val rssFilingIngestor: RssFilingIngestor,
) {

    private val executor = Executors.newCachedThreadPool()

    @GetMapping("{cik}/all-facts")
    fun allFactsForCik(@PathVariable cik: String): List<Fact> {
        return factBase.getFacts(cik)
    }

    @PostMapping("rss-filing-ingestor")
    fun runRssFilingIngestor(
        @RequestParam(required = false) numYearsToLookback: Int? = null,
    ) {
        executor.execute {
            try {
                rssFilingIngestor.run(numYearsToLookback = numYearsToLookback)
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
