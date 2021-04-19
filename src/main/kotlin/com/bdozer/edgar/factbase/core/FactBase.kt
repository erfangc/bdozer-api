package com.bdozer.edgar.factbase.core

import com.bdozer.edgar.factbase.FactExtensions.dimensions
import com.bdozer.edgar.factbase.FactExtensions.filterForDimensions
import com.bdozer.edgar.factbase.FactExtensions.filterForDimensionsWithFallback
import com.bdozer.edgar.factbase.core.support.FactsBootstrapper
import com.bdozer.edgar.factbase.dataclasses.AggregatedFact
import com.bdozer.edgar.factbase.dataclasses.Dimension
import com.bdozer.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.bdozer.edgar.factbase.dataclasses.Fact
import com.bdozer.extensions.DoubleExtensions.orZero
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.*
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class FactBase(
    mongoDatabase: MongoDatabase,
    private val factsBootstrapper: FactsBootstrapper,
) {

    private val facts = mongoDatabase.getCollection<Fact>()

    fun bootstrapFacts(cik: String) = factsBootstrapper.bootstrapFacts(cik)

    fun getFacts(cik: String): List<Fact> = facts.find(Fact::cik eq cik).toList()

    /* ------------------------
    Start of retrieval methods
     ------------------------ */

    /**
     * Get time series of facts based using a single [factId]
     * as template. Dimension of the fact referenced by the passed in [factId] will be used
     * and no fallback will be performed
     */
    fun getAnnualTimeSeries(factId: String): List<Fact> {
        val fact = getFact(factId) ?: error("...")
        return facts
            .find(
                and(
                    Fact::cik eq fact.cik,
                    Fact::conceptName eq fact.conceptName,
                    Fact::documentFiscalPeriodFocus eq fact.documentFiscalPeriodFocus,
                )
            )
            .toList()
            .filterForDimensions(fact.dimensions())
            .sortedByDescending { it.documentPeriodEndDate }

    }

    /**
     * Get time series of facts based using a single [factIds]
     * as template. Dimension of the fact referenced by the passed in [factIds] will be used
     * and no fallback will be performed
     *
     * @param factIds
     */
    fun getAnnualTimeSeries(factIds: List<String>): List<AggregatedFact> {
        val originalFacts = facts.find(Fact::_id `in` factIds).toList()
        val dimensions = originalFacts.dimensions()

        fun <R, K> List<R>.distinctOrThrow(fn: (R) -> K): K {
            val r = distinctBy(fn)
            when {
                r.isEmpty() -> {
                    error("set is empty")
                }
                r.size > 1 -> {
                    error("non distinct set found $r")
                }
                else -> {
                    return r.first().let(fn)
                }
            }
        }

        val cik = originalFacts.distinctOrThrow { it.cik }
        val conceptName = originalFacts.distinctOrThrow { it.conceptName }
        val documentFiscalPeriodFocus = originalFacts.distinctOrThrow { it.documentFiscalPeriodFocus }

        /*
        First gather all the facts
        */
        return facts
            .find(
                and(
                    Fact::cik eq cik,
                    Fact::conceptName eq conceptName,
                    Fact::documentFiscalPeriodFocus eq documentFiscalPeriodFocus,
                )
            )
            .toList()
            .filterForDimensions(dimensions)
            .groupBy { it.documentPeriodEndDate }
            .map { (documentPeriodEndDate, values) ->
                AggregatedFact(
                    factIds = values.map { it._id },
                    value = values.sumByDouble { it.doubleValue.orZero() }.orZero(),
                    conceptName = conceptName,
                    documentFiscalPeriodFocus = documentFiscalPeriodFocus,
                    documentPeriodEndDate = documentPeriodEndDate
                )
            }
            .sortedByDescending { it.documentPeriodEndDate }
    }

    /**
     * Get time series of facts for a single [conceptName] for a single filing entity.
     * Dimensions can be specified via [dimensions] parameter. If specified,
     * all facts queried must match the defined [dimensions]. Results are aggregated by date
     *
     * @param cik the filing entity identifier
     * @param dimensions the dimensions along which to query and filter the raw results
     *
     * @return a list of [Fact] aggregated by date. For a given period, data across multiple dimensions
     * are aggregated and thus not preserved
     */
    @Cacheable("getAnnualTimeSeries")
    fun getAnnualTimeSeries(
        cik: String,
        conceptName: String,
        dimensions: List<Dimension> = emptyList(),
        documentFiscalPeriodFocus: DocumentFiscalPeriodFocus = DocumentFiscalPeriodFocus.FY,
    ): List<AggregatedFact> {
        /*
        First gather all the facts
         */
        return facts
            .find(
                and(
                    Fact::cik eq cik,
                    Fact::conceptName eq conceptName,
                    Fact::documentFiscalPeriodFocus eq documentFiscalPeriodFocus,
                )
            )
            .toList()
            .filterForDimensionsWithFallback(dimensions)
            .groupBy { fact -> fact.documentPeriodEndDate }
            .map { (documentPeriodEndDate, values) ->
                AggregatedFact(
                    factIds = values.map { fact -> fact._id },
                    value = values.sumByDouble { it.doubleValue.orZero() },
                    conceptName = conceptName,
                    documentFiscalPeriodFocus = documentFiscalPeriodFocus,
                    documentPeriodEndDate = documentPeriodEndDate,
                )
            }
            .sortedByDescending { it.documentPeriodEndDate }
    }

    /**
     * Get a single fact for the given concept on the provided date or latest (if a end date is not provided)
     * The goal is to get the truest value at the concept level for a given concept. Business logic is as follows
     *
     *  - If [documentPeriodEndDate] is not provided, then the latest data will be provided
     *  - If [dimensions] is non-blank, then if a direct query for [conceptName] fails, we will
     *  build it back up using the provided dimensions
     */
    @Cacheable("getFact")
    fun getFactAggregated(
        cik: String,
        conceptName: String,
        documentFiscalPeriodFocus: DocumentFiscalPeriodFocus = DocumentFiscalPeriodFocus.FY,
        dimensions: List<Dimension> = emptyList(),
        documentPeriodEndDate: LocalDate? = null,
    ): AggregatedFact {

        val facts = facts
            .find(
                and(
                    Fact::cik eq cik,
                    Fact::conceptName eq conceptName,
                    Fact::documentFiscalPeriodFocus eq documentFiscalPeriodFocus,
                    documentPeriodEndDate?.let { Fact::documentPeriodEndDate eq it },
                )
            )
            .toList()

        /*
        apply filters as necessary
         */
        val filteredFacts = facts
            .filterForDimensions(dimensions)
            .groupBy { it.documentPeriodEndDate }
            .entries.maxByOrNull { it.key } ?: error("...")

        return AggregatedFact(
            factIds = listOf(),
            value = filteredFacts.value.sumByDouble { it.doubleValue.orZero() }.orZero(),
            conceptName = conceptName,
            documentFiscalPeriodFocus = documentFiscalPeriodFocus,
            documentPeriodEndDate = filteredFacts.key,
        )
    }

    /**
     * Get all the facts matching the params passed through the
     * function without any intelligent merging or fallback or date
     * operation logic
     *
     * @param cik the filing entity's CIK
     * @param conceptName the conceptName to get facts for
     * @param documentFiscalPeriodFocus the Period focus to get the facts for
     *
     * @return all matched [Fact] instances from FactBase
     */
    @Cacheable("getFacts")
    fun getFacts(
        cik: String,
        documentFiscalPeriodFocus: DocumentFiscalPeriodFocus? = null,
        documentPeriodEndDate: LocalDate? = null,
        conceptName: String? = null,
    ): List<Fact> {
        return facts
            .find(
                and(
                    Fact::cik eq cik,
                    documentFiscalPeriodFocus?.let { Fact::documentFiscalPeriodFocus eq it },
                    documentPeriodEndDate?.let { Fact::documentPeriodEndDate eq it },
                    conceptName?.let { Fact::conceptName eq it }
                )
            )
            .sort(descending(Fact::documentPeriodEndDate))
            .toList()
    }

    /**
     * This is the most direct way to return a fact by Id
     */
    fun getFact(factId: String): Fact? {
        return facts.findOneById(factId)
    }

    /* ------------------------
    End of retrieval methods
     ------------------------ */

    fun deleteAll(cik: String) =
        facts.deleteMany(Fact::cik eq cik.padStart(10, '0'))

}
