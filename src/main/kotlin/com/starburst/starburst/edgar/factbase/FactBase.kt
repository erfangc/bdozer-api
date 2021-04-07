package com.starburst.starburst.edgar.factbase

import com.mongodb.client.MongoDatabase
import com.starburst.starburst.extensions.DoubleExtensions.orZero
import com.starburst.starburst.edgar.factbase.dataclasses.*
import com.starburst.starburst.edgar.factbase.support.FactComponentFinder
import com.starburst.starburst.edgar.factbase.support.FactsBootstrapper
import org.litote.kmongo.*
import org.springframework.stereotype.Service

@Service
class FactBase(
    mongoDatabase: MongoDatabase,
    private val factComponentFinder: FactComponentFinder,
    private val factsBootstrapper: FactsBootstrapper,
) {

    private val facts = mongoDatabase.getCollection<Fact>()
    private val filingCalculations = mongoDatabase.getCollection<FilingCalculations>()

    fun bootstrapFacts(cik: String) = factsBootstrapper.bootstrapFacts(cik)

    fun factComponents(cik: String, conceptId: String): FindFactComponentsResponse =
        factComponentFinder.findFactComponents(cik, conceptId)

    fun calculations(cik: String) = filingCalculations
        .find(and(FilingCalculations::cik eq cik, FilingCalculations::formType eq "10-K"))
        .sort(descending(FilingCalculations::documentPeriodEndDate))
        .first() ?: error("no 10-K based calculation found for $cik")

    fun getFactTimeSeries(factId: String): FactTimeSeries {
        val originalFact = facts
            .findOneById(factId)
            ?: error("no fact can be found for id $factId")

        val cik = originalFact.cik
        val conceptName = originalFact.conceptName

        val filter = and(
            Fact::cik eq cik,
            Fact::conceptName eq conceptName,
            Fact::explicitMembers eq emptyList()
        )

        val factTimeSeries = facts
            .find(filter)
            .sort(descending(Fact::documentPeriodEndDate))
            .toList()

        val fyFacts = factTimeSeries
            .filter { fact -> fact.documentFiscalPeriodFocus == DocumentFiscalPeriodFocus.FY }

        val quarterlyFacts = factTimeSeries
            .filter { fact -> fact.documentFiscalPeriodFocus != DocumentFiscalPeriodFocus.FY }

        val ltmFacts = mutableListOf<Fact>()
        for (i in (0 until quarterlyFacts.size - 3)) {
            val minus0 = quarterlyFacts[i]?.doubleValue.orZero()
            val minus1 = quarterlyFacts[i + 1]?.doubleValue.orZero()
            val minus2 = quarterlyFacts[i + 2]?.doubleValue.orZero()
            val minus3 = quarterlyFacts[i + 3]?.doubleValue.orZero()
            ltmFacts.add(
                quarterlyFacts[i].copy(doubleValue = minus0 + minus1 + minus2 + minus3)
            )
        }

        return FactTimeSeries(
            fyFacts, quarterlyFacts, ltmFacts
        )
    }

    fun getFacts(cik: String): List<Fact> = facts.find(Fact::cik eq cik).toList()

    fun getFacts(
        cik: String,
        documentFiscalPeriodFocus: DocumentFiscalPeriodFocus? = null,
        conceptName: String? = null,
    ): List<Fact> {
        return facts
            .find(
                and(
                    Fact::cik eq cik,
                    documentFiscalPeriodFocus?.let { Fact::documentFiscalPeriodFocus eq it },
                    conceptName?.let { Fact::conceptName eq it }
                )
            )
            .sort(descending(Fact::documentPeriodEndDate))
            .toList()
    }

    fun deleteAll(cik: String) =
        facts.deleteMany(Fact::cik eq cik.padStart(10, '0'))

}
