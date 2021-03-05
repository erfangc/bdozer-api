package com.starburst.starburst.edgar.factbase

import com.starburst.starburst.edgar.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.ingestor.FilingIngestionResponse
import com.starburst.starburst.edgar.factbase.ingestor.FilingIngestor
import com.starburst.starburst.edgar.factbase.modelbuilder.ModelBuilder
import com.starburst.starburst.models.Model
import org.springframework.web.bind.annotation.*
import java.util.concurrent.Executors

@RestController
@RequestMapping("api/fact-base")
@CrossOrigin
class FactBaseController(
    private val factBase: FactBase,
    private val filingIngestor: FilingIngestor,
    private val modelBuilder: ModelBuilder
) {

    private val executor = Executors.newCachedThreadPool()

    @GetMapping("{cik}/latest-non-dimensional-facts")
    fun latestNonDimensionalFacts(@PathVariable cik: String): Map<String, Fact> {
        return factBase.latestNonDimensionalFacts(cik)
    }

    @GetMapping("{cik}/all-facts")
    fun allFactsForCik(@PathVariable cik: String): List<Fact> {
        return factBase.allFactsForCik(cik)
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

    @GetMapping("model-builder/{cik}/{adsh}")
    fun buildModelForFiling(@PathVariable adsh: String, @PathVariable cik: String): Model {
        return modelBuilder.buildModelForFiling(cik, adsh)
    }
}
