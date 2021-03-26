package com.starburst.starburst.marketing

import com.mongodb.client.MongoDatabase
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("public/marketing")
@CrossOrigin
class MarketingController(database: MongoDatabase) {

    val earlyAccessRequests = database.getCollection<EarlyAccessRequest>()
    val stockAnalysisRequest = database.getCollection<StockAnalysisRequest>()
    val stockAnalysisInterest = database.getCollection<StockAnalysisInterest>()


    @PostMapping("early-access-requests")
    fun earlyAccessRequests(@RequestBody body: EarlyAccessRequest) {
        earlyAccessRequests.save(body)
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