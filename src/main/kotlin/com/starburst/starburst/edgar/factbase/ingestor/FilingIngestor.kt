package com.starburst.starburst.edgar.factbase.ingestor

import com.mongodb.client.MongoClient
import com.starburst.starburst.edgar.dataclasses.Fact
import com.starburst.starburst.edgar.provider.FilingProviderFactory
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class FilingIngestor(
    mongoClient: MongoClient,
    private val filingProviderFactory: FilingProviderFactory
) {
    private val  log = LoggerFactory.getLogger(FilingIngestor::class.java)
    private val col = mongoClient
        .getDatabase("starburst")
        .getCollection<Fact>()
    private val executor = Executors.newCachedThreadPool()
    /**
     * Parse and save to database a given SEC EDGAR filing's XBRL files
     * given the CIK and ADSH
     */
    fun ingestFiling(
        cik: String,
        adsh: String
    ): FilingIngestionResponse {
        log.info("Parsing facts from cik=$cik and adsh=$adsh")
        val parser = FilingParser(filingProviderFactory.createFilingProvider(cik, adsh))
        val facts = parser.parseFacts()
        val distinctIds = facts.distinctBy { it._id }.size
        executor.submit {
            log.info("Saving ${facts.size} facts, ($distinctIds distinct) parsed for cik=$cik and adsh=$adsh")
            for (fact in facts) {
                // for some reason bulk write gets stuck
                col.save(fact)
            }
            log.info("Saved ${facts.size} facts parsed for cik=$cik and adsh=$adsh")
        }
        return FilingIngestionResponse(facts.size)
    }

}