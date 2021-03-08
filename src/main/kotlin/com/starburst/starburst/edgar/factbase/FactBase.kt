package com.starburst.starburst.edgar.factbase

import com.mongodb.client.MongoClient
import com.starburst.starburst.edgar.dataclasses.Fact
import com.starburst.starburst.edgar.dataclasses.XbrlExplicitMember
import com.starburst.starburst.models.HistoricalValue
import com.starburst.starburst.models.HistoricalValues
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.springframework.stereotype.Service
import java.time.LocalDate
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
     * Query the latest non-dimensional facts
     */
    fun latestNonDimensionalFacts(cik: String): Map<String, Fact> {
        val latestFacts = col.find(
            and(
                Fact::cik eq cik,
            )
        ).filter {
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

    companion object {

        private fun List<Fact>.ltm(
            elementName: String,
            explicitMembers: List<XbrlExplicitMember> = emptyList()
        ): HistoricalValue? {
            /*
            1 - figure out the latest date for which there is data
             */
            val latestFact = this
                .filter { fact -> fact.elementName == elementName && fact.explicitMembers == explicitMembers }
                .maxByOrNull { fact ->
                    fact.documentPeriodEndDate
                } ?: return null

            // instant facts do not require summation
            if (
                latestFact.documentFiscalPeriodFocus == "FY"
                ||
                latestFact.period.instant != null
            ) {
                /*
                return the latest FY figure
                 */
                val period = latestFact.period

                return HistoricalValue(
                    factId = latestFact._id,
                    documentFiscalYearFocus = latestFact.documentFiscalYearFocus,
                    documentFiscalPeriodFocus = latestFact.documentFiscalPeriodFocus,
                    documentPeriodEndDate = latestFact.documentPeriodEndDate,
                    value = latestFact.doubleValue,
                    startDate = period.startDate?.toString(),
                    endDate = period.endDate?.toString(),
                    instant = period.instant?.toString(),
                )

            } else {
                /*
                sum up the previous 4 quarters
                 */
                val period = latestFact.period
                val quarters = this.quarterlyHistoricalValues(elementName, explicitMembers)
                val ltm = quarters
                    .sortedByDescending { it.documentPeriodEndDate }
                    .subList(0, min(4, quarters.size))
                    .sumByDouble { it.value ?: 0.0 }

                return HistoricalValue(
                    factId = latestFact._id,
                    documentFiscalYearFocus = latestFact.documentFiscalYearFocus,
                    documentFiscalPeriodFocus = latestFact.documentFiscalPeriodFocus,
                    documentPeriodEndDate = latestFact.documentPeriodEndDate,
                    value = ltm,
                    startDate = period.startDate?.toString(),
                    endDate = period.endDate?.toString(),
                    instant = period.instant?.toString(),
                )
            }
        }

        private fun List<Fact>.fiscalYearHistoricalValues(
            elementName: String,
            explicitMembers: List<XbrlExplicitMember> = emptyList()
        ): List<HistoricalValue> {
            return this
                .filter { fact ->
                    fact.explicitMembers == explicitMembers
                            && fact.elementName == elementName
                            && fact.documentFiscalPeriodFocus == "FY"
                }
                .map { fact ->
                    val period = fact.period
                    HistoricalValue(
                        factId = fact._id,
                        documentFiscalYearFocus = fact.documentFiscalYearFocus,
                        documentFiscalPeriodFocus = fact.documentFiscalPeriodFocus,
                        documentPeriodEndDate = fact.documentPeriodEndDate,
                        value = fact.doubleValue,
                        startDate = period.startDate?.toString(),
                        endDate = period.endDate?.toString(),
                        instant = period.instant?.toString(),
                    )
                }
                .sortedByDescending { fact ->
                    fact.documentPeriodEndDate
                }
        }

        private fun List<Fact>.quarterlyHistoricalValues(
            elementName: String,
            explicitMembers: List<XbrlExplicitMember> = emptyList()
        ): List<HistoricalValue> {
            val filter = this
                .filter { fact ->
                    fact.explicitMembers == explicitMembers
                            && fact.elementName == elementName
                            && fact.documentFiscalPeriodFocus.startsWith("Q")
                }
            return filter
                .map { fact ->
                    val period = fact.period
                    HistoricalValue(
                        factId = fact._id,
                        documentFiscalYearFocus = fact.documentFiscalYearFocus,
                        documentFiscalPeriodFocus = fact.documentFiscalPeriodFocus,
                        documentPeriodEndDate = fact.documentPeriodEndDate,
                        value = fact.doubleValue,
                        startDate = period.startDate?.toString(),
                        endDate = period.endDate?.toString(),
                        instant = period.instant?.toString(),
                    )
                }
                .sortedByDescending { fact ->
                    fact.documentPeriodEndDate
                }
        }

        fun List<Fact>.allHistoricalValues(
            elmName: String,
            explicitMembers: List<XbrlExplicitMember> = emptyList()
        ): HistoricalValues {
            return HistoricalValues(
                fiscalYear = fiscalYearHistoricalValues(elmName, explicitMembers),
                quarterly = quarterlyHistoricalValues(elmName, explicitMembers),
                ltm = ltm(elmName, explicitMembers),
            )
        }
    }

}