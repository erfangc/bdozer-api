package com.starburst.starburst.edgar.factbase

import com.mongodb.client.MongoClient
import com.starburst.starburst.edgar.dataclasses.Fact
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class FactBase(mongoClient: MongoClient) {

    private val col = mongoClient
        .getDatabase("starburst")
        .getCollection<Fact>()

    fun deleteAll(cik: String) {
        col.deleteMany(Fact::cik eq cik)
    }

    /**
     * Query the latest non-dimensional facts
     */
    fun latestNonDimensionalFacts(cik: String): Map<String, Fact> {
        val latestFacts = col.find(and(
            Fact::cik eq cik,
            Fact::canonical eq true
        )).filter {
            it.explicitMembers.isEmpty() && (
                    it.period.endDate == LocalDate.parse(it.documentPeriodEndDate)
            ||
                    it.period.instant == LocalDate.parse(it.documentPeriodEndDate)
            )
        }
            .groupBy { it.documentFiscalYearFocus }
            .entries.maxByOrNull { it.key }
            ?.value ?: emptyList()
        return latestFacts.associateBy { it.elementName }
    }

    /**
     * Query all the facts (across dimension and time) for a given entity
     * designated by the CIK
     */
    fun allFactsForCik(cik: String): List<Fact> {
        return col.find(Fact::cik eq cik).toList()
    }

}