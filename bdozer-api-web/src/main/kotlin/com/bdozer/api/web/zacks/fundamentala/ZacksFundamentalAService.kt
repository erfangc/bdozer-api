package com.bdozer.api.web.zacks.fundamentala

import com.mongodb.client.MongoDatabase
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.springframework.stereotype.Service

@Service
class ZacksFundamentalAService(mongoDatabase: MongoDatabase) {

    private val zacksFundamentalA = mongoDatabase.getCollection<ZacksFundamentalAWrapper>()

    fun getZacksFundamentalAs(ticker: String): List<ZacksFundamentalA> {
        return zacksFundamentalA
            .find(ZacksFundamentalAWrapper::content / ZacksFundamentalA::ticker eq ticker)
            .map { it.content }
            .toList()
    }

}