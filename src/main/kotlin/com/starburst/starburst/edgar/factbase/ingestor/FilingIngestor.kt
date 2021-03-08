package com.starburst.starburst.edgar.factbase.ingestor

import com.mongodb.client.MongoClient
import com.starburst.starburst.edgar.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.ingestor.q4.Q4FactFinder
import com.starburst.starburst.edgar.provider.FilingProviderFactory
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FilingIngestor(
    private val mongoClient: MongoClient,
    private val filingProviderFactory: FilingProviderFactory
) {
    private val log = LoggerFactory.getLogger(FilingIngestor::class.java)
    private val col = mongoClient
        .getDatabase("starburst")
        .getCollection<Fact>()

    /**
     * Parse and save to database a given SEC EDGAR filing's XBRL files
     * given the [cik] and [adsh]
     */
    fun ingestFiling(
        cik: String,
        adsh: String
    ): FilingIngestionResponse {
        log.info("Parsing facts from cik=$cik and adsh=$adsh")
        val parser = FilingParser(filingProviderFactory.createFilingProvider(cik, adsh))
        val resp = parser.parseFacts()
        val facts = resp.facts
        val distinctIds = facts.distinctBy { it._id }.size
        log.info("Saving ${facts.size} facts, ($distinctIds distinct) parsed for cik=$cik and adsh=$adsh")
        for (fact in facts) {
            // for some reason bulk write gets stuck
            col.save(fact)
        }
        log.info("Saved ${facts.size} facts parsed for cik=$cik and adsh=$adsh")
        return FilingIngestionResponse(
            numberOfFactsFound = facts.size,
            documentPeriodEndDate = resp.documentPeriodEndDate,
            documentFiscalPeriodFocus = resp.documentFiscalPeriodFocus,
            documentFiscalYearFocus = resp.documentFiscalYearFocus,
        )
    }

    /**
     * back-fill Q4 data given a cik and a fiscal year, using 10-K and the same FY 10-Qs to
     * reconstruct
     */
    fun ingestQ4Facts(cik: String, year: Int) {
        return Q4FactFinder(mongoClient = mongoClient).run(cik, year)
    }

}