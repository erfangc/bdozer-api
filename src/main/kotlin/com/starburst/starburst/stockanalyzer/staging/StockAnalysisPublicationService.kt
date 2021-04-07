package com.starburst.starburst.stockanalyzer.staging

import com.mongodb.client.MongoDatabase
import com.starburst.starburst.stockanalyzer.staging.dataclasses.StockAnalysis2
import org.litote.kmongo.deleteOneById
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.stereotype.Service

@Service
class StockAnalysisPublicationService(
    mongoDatabase: MongoDatabase
) {
    private val col = mongoDatabase.getCollection<StockAnalysis2>("publishedStockAnalysis")

    fun publish(stockAnalysis: StockAnalysis2) {
        col.save(stockAnalysis)
    }

    fun unpublish(id: String) {
        col.deleteOneById(id)
    }

    fun find(skip: Int = 0, limit: Int = 10): List<StockAnalysis2> {
        return col
            .find()
            .skip(skip)
            .limit(limit)
            .toList()
    }

    fun get(id: String): StockAnalysis2? {
        return col.findOneById(id)
    }
    
}