package com.starburst.starburst.edgar.factbase.support

import com.mongodb.client.MongoDatabase
import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.dataclasses.FilingCalculations
import com.starburst.starburst.edgar.factbase.dataclasses.FindFactComponentsResponse
import org.litote.kmongo.and
import org.litote.kmongo.descending
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.springframework.stereotype.Service
import java.net.URI

/**
 * [FactComponentFinder] takes a concept defined by a filer and
 * find it's components (if any) in the EDGAR based fact base
 * and return all of the historical data associated with those components
 *
 * Ex:
 * - OperatingExpense -> SG&A, Selling and Marketing + Restructuring
 */
@Service
class FactComponentFinder(
    mongoDatabase: MongoDatabase,
) {

    private val factsCol = mongoDatabase.getCollection<Fact>()
    private val calculationsCol = mongoDatabase.getCollection<FilingCalculations>()

    fun findFactComponents(cik: String, conceptId: String): FindFactComponentsResponse {

        val filingCalculations = latestFilingCalculations(cik)
        val calculations = calculations(filingCalculations, conceptId)
        val conceptHrefs = calculations.map { URI(it.conceptHref).fragment }

        val facts = factsCol
            .find(Fact::cik eq cik)
            .filter { fact -> conceptHrefs.contains(URI(fact.conceptHref).fragment) }

        return FindFactComponentsResponse(
            componentFacts = facts,
            calculations = calculations,
            latestAnnual = latestAnnualFacts(facts),
            latestQuarterly = latestQuarterFacts(facts)
        )
    }

    private fun calculations(
        filingCalculations: FilingCalculations?,
        conceptId: String
    ) = filingCalculations
        ?.incomeStatement
        ?.find { URI(it.conceptHref).fragment == conceptId }
        ?.calculations ?: emptyList()

    private fun latestFilingCalculations(cik: String) = calculationsCol
        .find(
            and(FilingCalculations::cik eq cik, FilingCalculations::formType eq "10-K")
        )
        .sort(descending(FilingCalculations::documentPeriodEndDate))
        .first()

    private fun latestQuarterFacts(facts: List<Fact>): List<Fact> {
        return facts
            .filter { it.formType == "10-Q" }
            .groupBy { it.documentPeriodEndDate }
            .maxByOrNull { it.key }?.value ?: emptyList()
    }

    private fun latestAnnualFacts(facts: List<Fact>): List<Fact> {
        return facts
            .filter { it.formType == "10-K" }
            .groupBy { it.documentPeriodEndDate }
            .maxByOrNull { it.key }?.value ?: emptyList()
    }

}