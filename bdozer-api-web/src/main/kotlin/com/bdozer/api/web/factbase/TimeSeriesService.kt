package com.bdozer.api.web.factbase

import com.bdozer.api.factbase.core.dataclasses.DocumentFiscalPeriodFocus
import com.bdozer.api.factbase.core.dataclasses.Fact
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.*
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * [TimeSeriesService] query time series data points for concepts and allow for their comparison
 */
@Service
class TimeSeriesService(mongoDatabase: MongoDatabase) {

    private val col = mongoDatabase.getCollection<Fact>()

    /**
     * Grab time series
     */
    fun getTimeSeriesForFact(
        cik: String,
        factId: String,
        conceptNames: List<String>,
        startDate: LocalDate,
        stopDate: LocalDate,
        documentFiscalPeriodFocus: DocumentFiscalPeriodFocus,
        prune: Boolean = false,
    ): List<FactTimeSeries> {

        val fact = col.findOneById(factId) ?: return emptyList()
        val conceptName = fact.conceptName

        val filters = and(
            Fact::cik eq cik,
            Fact::conceptName eq conceptName,
            Fact::documentPeriodEndDate gte startDate,
            Fact::documentPeriodEndDate lte stopDate,
        )
        val facts = col.find(filters).toList().filter { found ->
            if (fact.explicitMembers.isEmpty()) {
                found.explicitMembers.isEmpty()
            } else {
                found.explicitMembers.any { inner ->
                    fact.explicitMembers.any { outer ->
                        outer.dimension == inner.dimension && outer.value == inner.value
                    }
                }
            }
        }

        val timeSeries = getTimeSeries(
            cik = cik,
            conceptNames = conceptNames,
            startDate = startDate,
            stopDate = stopDate,
            documentFiscalPeriodFocus = documentFiscalPeriodFocus,
            prune = prune,
        )

        return if (prune) {
            pruneFts(
                timeSeries + FactTimeSeries(
                    facts = facts,
                    conceptName = fact.conceptName,
                    label = fact.label(),
                    startDate = startDate,
                    stopDate = stopDate,
                    documentFiscalPeriodFocus = documentFiscalPeriodFocus,
                )
            )
        } else {
            timeSeries + FactTimeSeries(
                facts = facts,
                conceptName = fact.conceptName,
                label = facts.firstOrNull()?.label(),
                startDate = startDate, stopDate = stopDate,
                documentFiscalPeriodFocus = documentFiscalPeriodFocus,
            )
        }
    }

    fun getTimeSeries(
        cik: String,
        conceptNames: List<String>,
        startDate: LocalDate,
        stopDate: LocalDate,
        documentFiscalPeriodFocus: DocumentFiscalPeriodFocus,
        prune: Boolean = false,
    ): List<FactTimeSeries> {

        val filters = and(
            Fact::cik eq cik,
            Fact::conceptName `in` conceptNames,
            Fact::documentPeriodEndDate gte startDate,
            Fact::documentPeriodEndDate lte stopDate,
            Fact::documentFiscalPeriodFocus eq documentFiscalPeriodFocus,
        )

        val factTimeSeries = col
            .find(filters)
            .groupBy { it.conceptName }
            .map { (conceptName, facts) ->
                facts.filter { fact ->
                    // TODO add parameters to enable filtering on explicit members
                    fact.explicitMembers.isEmpty()
                }
                val stopDate = facts.maxOf { fact -> fact.documentPeriodEndDate }
                val startDate = facts.minOf { fact -> fact.documentPeriodEndDate }
                FactTimeSeries(
                    facts = facts,
                    conceptName = conceptName,
                    startDate = startDate,
                    stopDate = stopDate,
                    label = facts.firstOrNull()?.label(),
                    documentFiscalPeriodFocus = documentFiscalPeriodFocus,
                )
            }

        return if (prune) {
            pruneFts(factTimeSeries)
        } else {
            factTimeSeries
        }
    }

    private fun pruneFts(factTimeSeries: List<FactTimeSeries>): List<FactTimeSeries> {
        if (factTimeSeries.isEmpty()) {
            return emptyList()
        }
        /*
        prune the time series so they align
         */
        val uniqueDates = factTimeSeries.map { fts ->
            fts.facts.map { it.documentPeriodEndDate }.toSet()
        }.reduceRight { set, acc ->
            acc.intersect(set)
        }
        // only consider facts whose 'date' are in the intersect of all time series
        return factTimeSeries.map { fts ->
            fts.copy(
                facts = fts.facts.filter { uniqueDates.contains(it.documentPeriodEndDate) }
            )
        }
    }

}