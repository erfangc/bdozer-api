package com.starburst.starburst.edgar.factbase.ingestor

import com.mongodb.client.MongoDatabase
import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.ingestor.dataclasses.FilingCalculations
import com.starburst.starburst.edgar.factbase.ingestor.dataclasses.FilingIngestionResponse
import com.starburst.starburst.edgar.factbase.ingestor.q4.Q4FactFinder
import com.starburst.starburst.edgar.factbase.ingestor.support.CalculationsParser
import com.starburst.starburst.edgar.factbase.ingestor.support.FactsParser
import com.starburst.starburst.edgar.provider.FilingProviderFactory
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.lang.Exception

@Service
class FilingIngestor(
    mongoDatabase: MongoDatabase,
    private val filingProviderFactory: FilingProviderFactory,
    private val q4FactFinder: Q4FactFinder,
) {
    private val log = LoggerFactory.getLogger(FilingIngestor::class.java)
    private val factsCol = mongoDatabase.getCollection<Fact>()
    private val calculationsCol = mongoDatabase.getCollection<FilingCalculations>()

    /**
     * Parse and save to database a given SEC EDGAR filing's XBRL files
     * given the [cik] and [adsh]
     */
    fun ingestFiling(cik: String, adsh: String): FilingIngestionResponse {
        val filingProvider = filingProviderFactory.createFilingProvider(cik, adsh)

        /*
        Parse and save the facts
         */
        log.info("Parsing facts from cik=$cik and adsh=$adsh")
        val factsParser = FactsParser(filingProvider)
        val resp = factsParser.parseFacts()
        val facts = resp.facts
        val distinctIds = facts.distinctBy { it._id }.size
        log.info("Saving ${facts.size} facts, ($distinctIds distinct) parsed for cik=$cik and adsh=$adsh")
        // try to leverage bulk write somehow here
        for (fact in facts) {
            factsCol.save(fact)
        }
        log.info("Saved ${facts.size} facts parsed for cik=$cik and adsh=$adsh")

        /*
        Parse and save the calculations
         */
        try {
            val calculationsParser = CalculationsParser(filingProvider = filingProvider)
            val calculations = calculationsParser.parseCalculations()
            calculationsCol.save(calculations)
        } catch (e: Exception) {
            log.error("Unable to parse and save calculations for cik=$cik, adsh=$adsh",e)
        }

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
        return q4FactFinder.run(cik, year)
    }

}