package com.starburst.starburst.edgar.factbase

import com.mongodb.client.MongoDatabase
import com.starburst.starburst.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.dataclasses.FilingCalculations
import com.starburst.starburst.edgar.factbase.dataclasses.FindFactComponentsResponse
import com.starburst.starburst.edgar.factbase.ingestor.dataclasses.Arc
import com.starburst.starburst.edgar.factbase.support.FactComponentFinder
import com.starburst.starburst.edgar.factbase.support.FactsBootstrapper
import com.starburst.starburst.edgar.factbase.support.LabelManager
import com.starburst.starburst.models.dataclasses.HistoricalValue
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.Model
import org.litote.kmongo.and
import org.litote.kmongo.descending
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.springframework.stereotype.Service
import java.net.URI

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

    fun getFacts(cik: String): List<Fact> {
        return facts.find(Fact::cik eq cik).toList()
    }

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
