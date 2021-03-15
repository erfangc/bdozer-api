package com.starburst.starburst.edgar.factbase

import com.mongodb.client.MongoClient
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.springframework.stereotype.Service

@Service
class FactBase(mongoClient: MongoClient) {

    private val col = mongoClient
        .getDatabase("starburst")
        .getCollection<Fact>()

    fun deleteAll(cik: String) {
        col.deleteMany(Fact::cik eq cik)
    }

    /**
     * Query all the facts (across dimension and time) for a given entity
     * designated by the CIK
     */
    fun allFactsForCik(cik: String): List<Fact> {
        return col.find(Fact::cik eq cik).toList()
    }

}