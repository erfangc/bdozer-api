package com.starburst.starburst.marketing

import com.mongodb.client.MongoDatabase
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

data class Survey(
    val _id: String = UUID.randomUUID().toString(),
    val body: Any,
    val lastUpdated: Instant = Instant.now(),
    val version: String = "N/A",
)

@RestController
@RequestMapping("public/marketing")
@CrossOrigin
class MarketingController(database: MongoDatabase) {

    val earlyAccessRequests = database.getCollection<EarlyAccessRequest>()
    val stockAnalysisRequest = database.getCollection<StockAnalysisRequest>()
    val stockAnalysisInterest = database.getCollection<StockAnalysisInterest>()
    val surveys = database.getCollection<Survey>()


    @PostMapping("early-access-requests")
    fun earlyAccessRequests(@RequestBody body: EarlyAccessRequest) {
        earlyAccessRequests.save(body)
    }


    @PostMapping("2021-03-29/survey")
    fun survey20210329(@RequestBody survey: Survey) {
        surveys.save(survey.copy(version = "2021-03-29"))
    }

    @PostMapping("stock-analysis-request")
    fun stockAnalysisRequest(@RequestBody body: List<StockAnalysisRequest>) {
        body.forEach {
            stockAnalysisRequest.save(it)
        }
    }

    @PostMapping("stock-analysis-interest")
    fun stockAnalysisInterest(@RequestBody body: StockAnalysisInterest) {
        stockAnalysisInterest.save(body)
    }

}