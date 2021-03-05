package com.starburst.starburst.edgar.factbase

import com.mongodb.client.MongoClient
import com.starburst.starburst.edgar.dataclasses.Fact
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class FactBase(mongoClient: MongoClient) {

    private val col = mongoClient
        .getDatabase("starburst")
        .getCollection<Fact>()

    /**
     * Query the latest non-dimensional facts
     */
    fun latestNonDimensionalFacts(cik: String): Map<String, Fact> {
        val factsByPeriod = col.find(Fact::cik eq cik).groupBy { it.period }

        // latest duration
        val latestDuration = factsByPeriod.entries.maxByOrNull { it.key.endDate ?: LocalDate.MIN }?.value ?: emptyList()

        // latest instant
        val latestInstant = factsByPeriod.entries.maxByOrNull { it.key.instant ?: LocalDate.MIN }?.value ?: emptyList()

        return (latestDuration + latestInstant)
            .filter { it.explicitMembers.isEmpty() }
            .associateBy { it.elementName }
    }

    /**
     * Query all the facts (across dimension and time) for a given entity
     * designated by the CIK
     */
    fun allFactsForCik(cik: String): List<Fact> {
        return col.find(
            Fact::cik eq cik
        ).toList()
    }

}