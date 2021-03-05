package com.starburst.starburst.edgar.factbase

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.MongoClient
import com.starburst.starburst.edgar.dataclasses.Fact
import com.starburst.starburst.edgar.provider.FilingProviderImpl
import org.apache.http.client.HttpClient
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.concurrent.Executors

@Service
class FactBase(
    private val http: HttpClient,
    private val objectMapper: ObjectMapper,
    mongoClient: MongoClient
) {

    private val log = LoggerFactory.getLogger(FactBase::class.java)

    private val database = mongoClient.getDatabase("starburst")
    private val col = database.getCollection<Fact>()
    private val executor = Executors.newCachedThreadPool()

    /**
     *
     */
    fun searchFacts(
        term: String,
        cik: String? = null,
        dimension: String? = null,
        formType: String? = null
    ): List<Fact> {
        TODO()
    }

    /**
     * Query the latest non-dimensional facts
     */
    fun getLatestNonDimensionalFacts(cik: String): Map<String, Fact> {
        val byPeriod = col.find(Fact::cik eq cik).groupBy { it.period }
        // latest duration
        val latestDuration = byPeriod.entries.maxByOrNull { it.key.endDate ?: LocalDate.MIN }?.value ?: emptyList()

        // latest instant
        val latestInstant = byPeriod.entries.maxByOrNull { it.key.instant ?: LocalDate.MIN }?.value ?: emptyList()

        return (latestDuration + latestInstant).filter { it.explicitMembers.isEmpty() }.associateBy { it.nodeName }
    }

    /**
     * Query all the facts (across dimension and time) for a given entity
     * designated by the CIK
     */
    fun getAllFactsForCik(cik: String): List<Fact> {
        return col.find(
            Fact::cik eq cik
        ).toList()
    }

    /**
     * Parse and save to database a given SEC EDGAR filing's XBRL files
     * given the CIK and ADSH
     */
    fun parseAndUploadSingleFiling(
        cik: String,
        adsh: String
    ): ParseUploadSingleFilingResponse {
        log.info("Parsing facts from cik=$cik and adsh=$adsh")
        val impl = FilingProviderImpl(cik, adsh, http, objectMapper)
        val parser = FaceBaseFilingParser(impl)
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
        return ParseUploadSingleFilingResponse(facts.size)
    }

}