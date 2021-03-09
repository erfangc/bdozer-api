package com.starburst.starburst.edgar.factbase

import com.mongodb.client.MongoClient
import com.starburst.starburst.edgar.dataclasses.Fact
import com.starburst.starburst.edgar.dataclasses.XbrlExplicitMember
import com.starburst.starburst.models.HistoricalValue
import com.starburst.starburst.models.HistoricalValues
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.springframework.stereotype.Service
import kotlin.math.min

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