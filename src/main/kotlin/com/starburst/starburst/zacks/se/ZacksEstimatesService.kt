package com.starburst.starburst.zacks.se

import com.mongodb.client.MongoDatabase
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.springframework.stereotype.Service

@Service
class ZacksEstimatesService(mongoDatabase: MongoDatabase) {

    private val zacksSalesEstimates = mongoDatabase.getCollection<ZacksSalesEstimatesWrapper>()

    fun getZacksSaleEstimates(ticker: String): List<ZacksSalesEstimates> {
        return zacksSalesEstimates
            .find(ZacksSalesEstimatesWrapper::content / ZacksSalesEstimates::ticker eq ticker)
            .map { it.content }
            .toList()
    }

}