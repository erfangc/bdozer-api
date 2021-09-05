package com.bdozer.api.web.stockanalysisrequest

import com.mongodb.client.MongoDatabase
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.stereotype.Service

@Service
class StockAnalysisRequestService(
    mongoDatabase: MongoDatabase,
) {
    private val stockAnalysisRequests = mongoDatabase.getCollection<StockAnalysisRequest>()

    fun saveStockAnalysisRequest(ticker: String, email: String) {
        stockAnalysisRequests.save(StockAnalysisRequest(ticker = ticker, email = email))
    }
}