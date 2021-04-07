package com.starburst.starburst.stockanalyzer.staging

import com.mongodb.client.MongoDatabase
import com.starburst.starburst.stockanalyzer.staging.dataclasses.StockAnalysis2
import org.litote.kmongo.*
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class StockAnalysisCRUDService(mongoDatabase: MongoDatabase) {

    val col = mongoDatabase.getCollection<StockAnalysis2>()

    fun save(analysis: StockAnalysis2) {
        col.save(analysis.copy(lastUpdated = Instant.now()))
    }

    fun delete(id: String) {
        col.deleteOneById(id)
    }

    fun find(
        userId: String? = null,
        cik: String? = null,
        ticker: String? = null,
    ): List<StockAnalysis2> {
        val filter = and(
            userId?.let { StockAnalysis2::userId eq it },
            cik?.let { StockAnalysis2::cik eq it },
            ticker?.let { StockAnalysis2::ticker eq it },
        )
        return col
            .find(filter)
            .sort(descending(StockAnalysis2::lastUpdated))
            .toList()
    }

}