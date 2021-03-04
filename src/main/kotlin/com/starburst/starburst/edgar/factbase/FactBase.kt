package com.starburst.starburst.edgar.factbase

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.MongoClient
import com.starburst.starburst.edgar.dataclasses.Fact
import com.starburst.starburst.edgar.provider.FilingProviderImpl
import org.apache.http.client.HttpClient
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
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
     * Returns a list of facts matching the query
     */
    fun queryFacts(
        cik: String,
        nodeName: String,
        dimension: String? = null
    ): List<Fact> {
        return col.find(Fact::nodeName eq nodeName).filter { fact ->
            fact.explicitMembers.any { it.dimension == dimension } || dimension == null
        }.toList()
    }

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