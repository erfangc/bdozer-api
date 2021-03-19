package com.starburst.starburst.edgar.factbase

import com.mongodb.client.MongoDatabase
import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.dataclasses.FindFactComponentsResponse
import com.starburst.starburst.edgar.factbase.support.FactComponentFinder
import com.starburst.starburst.edgar.factbase.support.FactsBootstrapper
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.springframework.stereotype.Service

@Service
class FactBase(
    mongoDatabase: MongoDatabase,
    private val factComponentFinder: FactComponentFinder,
    private val factsBootstrapper: FactsBootstrapper,
) {

    private val col = mongoDatabase.getCollection<Fact>()

    fun bootstrapFacts(cik: String) {
        factsBootstrapper.bootstrapFacts(cik)
    }

    fun factComponents(cik: String, conceptId: String): FindFactComponentsResponse {
        return factComponentFinder.findFactComponents(cik, conceptId)
    }

    fun getFacts(cik: String): List<Fact> {
        return col.find(Fact::cik eq cik).toList()
    }

    fun deleteAll(cik: String) {
        col.deleteMany(Fact::cik eq cik)
    }

}