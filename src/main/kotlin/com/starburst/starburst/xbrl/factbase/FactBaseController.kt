package com.starburst.starburst.xbrl.factbase

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.MongoClient
import com.starburst.starburst.xbrl.FilingProviderImpl
import org.apache.http.client.HttpClient
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.util.concurrent.Executors

data class ParseUploadSingleFilingResponse(
    val numberOfFactsFound: Int
)

@RestController
@RequestMapping("api/fact-base")
@CrossOrigin
class FactBaseController(
    private val objectMapper: ObjectMapper,
    private val http: HttpClient,
    mongoClient: MongoClient
) {

    private val log = LoggerFactory.getLogger(FactBaseController::class.java)

    private val database = mongoClient.getDatabase("starburst")
    private val col = database.getCollection<Fact>()
    private val executor = Executors.newCachedThreadPool()

    @PostMapping("{cik}/{adsh}")
    fun parseAndUploadSingleFiling(
        @PathVariable cik: String,
        @PathVariable adsh: String
    ): ParseUploadSingleFilingResponse {
        log.info("Parsing facts from cik=$cik and adsh=$adsh")
        val impl = FilingProviderImpl(cik, adsh, http, objectMapper)
        val parser = XbrlFactParser(impl)
        val facts = parser.parseFacts()
        executor.submit {
            facts.forEach { fact ->
                col.save(fact)
            }
        }
        return ParseUploadSingleFilingResponse(facts.size)
    }

}
