package com.bdozer.sec.factbase.ingestor

import com.bdozer.sec.factbase.dataclasses.Fact
import com.bdozer.sec.factbase.filing.SECFilingFactory
import com.bdozer.sec.factbase.ingestor.dataclasses.FilingIngestionResponse
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.ReplaceOptions
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.replaceOne
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FilingIngestor(
    mongoDatabase: MongoDatabase,
    private val secFilingFactory: SECFilingFactory,
) {

    private val q4FactFinder = Q4FactFinder(mongoDatabase)
    private val log = LoggerFactory.getLogger(FilingIngestor::class.java)
    private val factsCol = mongoDatabase.getCollection<Fact>()

    /**
     * Parse and save to database a given SEC EDGAR filing's XBRL files
     * given the [cik] and [adsh]
     */
    fun ingestFiling(cik: String, adsh: String): FilingIngestionResponse {
        val secFiling = secFilingFactory.createSECFiling(cik, adsh)

        /*
        Parse and save the facts
         */
        log.info("Parsing facts from cik=$cik and adsh=$adsh")
        val factsParser = secFiling.factsParser
        val resp = factsParser.parseFacts()
        val facts = resp.facts
        val distinctIds = facts.distinctBy { it._id }.size
        log.info("Saving ${facts.size} facts, ($distinctIds distinct) parsed for cik=$cik and adsh=$adsh")
        facts
            .chunked(55)
            .forEach { chunk ->
                val bulk = chunk.map { replaceOne(Fact::_id eq it._id, it, ReplaceOptions().upsert(true)) }
                factsCol.bulkWrite(bulk)
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
        return q4FactFinder.run(cik, year)
    }

}