package com.bdozer.sec.factbase.ingestor

import com.bdozer.api.common.dataclasses.sec.*
import com.bdozer.sec.factbase.FactIdGenerator
import com.bdozer.api.common.dataclasses.sec.DocumentFiscalPeriodFocus
import com.bdozer.api.common.dataclasses.sec.Fact
import com.bdozer.extensions.DoubleExtensions.orZero
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.ReplaceOptions
import org.litote.kmongo.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.TemporalAdjusters.lastDayOfMonth

/**
 * After parsing 3 10-Qs per fiscal year and 1 10-K - we are now
 * missing the pure Q4 figures - for that we must actually retrieve
 * the 10-K figure and the past 3 10-Qs
 */
class Q4FactFinder(mongoDatabase: MongoDatabase) {

    private val col = mongoDatabase.getCollection<Fact>()
    private val log = LoggerFactory.getLogger(Q4FactFinder::class.java)

    /**
     * Run against FactBase and populate Q4 data
     */
    fun run(cik: String, fiscalYear: Int) {

        /*
        1 - grab all the facts of a given fiscal year
         */
        val facts = col.find(
            and(
                Fact::documentFiscalYearFocus eq fiscalYear,
                Fact::cik eq cik,
                Fact::documentFiscalPeriodFocus ne DocumentFiscalPeriodFocus.Q4
            )
        )

        /*
        2 - split the facts into the FY (10-K) facts and the reported 10-Q facts
         */
        val tenKFacts = facts.filter { fact ->
            fact.formType == "10-K"
        }

        val tenQs = facts.filter { fact ->
            fact.formType == "10-Q"
        }

        val quarters = tenQs
            .groupBy { fact -> toIdentityContext(fact) }
            .mapValues { (_, facts) ->
                // group each fact by context
                facts.associateBy { fact ->
                    fact.documentFiscalPeriodFocus
                }
            }

        /*
        go through each fact from the 10-K and try to replicate it
         */
        val q4Facts = tenKFacts.mapNotNull { fyFact ->

            val fyContext = toFyContext(fyFact)
            val identityContext = toIdentityContext(fyFact)
            val q4Context = q4Context(fyContext)

            /*
            we do not need to perform a FY value - sum(prev 3 quarter) computation
            if a) the fact is an instantaneous fact like an balance sheet item or
            b) if the fact is not of double value type
             */
            if (fyFact.doubleValue != null && fyContext.period.instant == null) {
                /*
                Derive q4 value
                 */
                val valuesByQ = quarters[identityContext] ?: emptyMap()

                try {
                    /*
                    Try to sum over the past 3 quarter's data and then subtract them from the 10-K
                     */
                    val value = fyFact.doubleValue.orZero() - listOf(
                        DocumentFiscalPeriodFocus.Q1,
                        DocumentFiscalPeriodFocus.Q2,
                        DocumentFiscalPeriodFocus.Q3,
                    ).sumByDouble { quarter ->
                        valuesByQ[quarter]?.doubleValue ?: 0.0
                    }

                    fyFact.copy(
                        _id = generateQ4FactId(fyFact),
                        formType = "10-Q",
                        documentFiscalPeriodFocus = DocumentFiscalPeriodFocus.Q4,
                        startDate = q4Context.period.startDate,
                        endDate = q4Context.period.endDate,
                        instant = q4Context.period.instant,
                        stringValue = value.toString(),
                        doubleValue = value,
                        lastUpdated = Instant.now().toString(),
                    )
                } catch (e: Exception) {
                    log.error("Error encountered", e)
                    null
                }
            } else {
                /*
                No need to derive Q4 values because the fact is instantaneous or not of double type
                 */
                fyFact.copy(
                    _id = generateQ4FactId(fyFact),
                    formType = "10-Q",
                    documentFiscalPeriodFocus = DocumentFiscalPeriodFocus.Q4,
                    startDate = q4Context.period.startDate,
                    endDate = q4Context.period.endDate,
                    instant = q4Context.period.instant,
                    lastUpdated = Instant.now().toString(),
                )
            }
        }
        q4Facts.chunked(55).forEach { chunk ->
            val bulk = chunk.map { replaceOne(Fact::_id eq it._id, it, ReplaceOptions().upsert(true)) }
            col.bulkWrite(bulk)
        }
        log.info("Saved ${q4Facts.size} Q4 facts for cik=$cik, fiscalYear=$fiscalYear")
    }

    private fun generateQ4FactId(fyFact: Fact): String {
        /*
        copy the context of fyFact but change the start date
         */
        val fyCtx = toFyContext(fyFact)
        return FactIdGenerator()
            .generateId(
                conceptName = fyFact.conceptName,
                context = q4Context(fyCtx),
                documentFiscalPeriodFocus = DocumentFiscalPeriodFocus.Q4,
            )
    }

    /**
     * Create the theoretical Q4 context:
     *
     * if the fyContext has an endDate and not an instant, then we need to figure out the start date
     * otherwise nothing about this context changes
     */
    private fun q4Context(fyCtx: XbrlContext): XbrlContext {
        val startDate = fyCtx.period.endDate
            ?.minusMonths(3)
            ?.with(lastDayOfMonth())
            ?.plusDays(1)
        return fyCtx.copy(period = fyCtx.period.copy(startDate = startDate))
    }

    /**
     * Reconstructs the FY fact's original context with a date on it
     * thus it is not an identity context
     */
    private fun toFyContext(fyFact: Fact): XbrlContext {
        return XbrlContext(
            id = "-",
            entity = XbrlEntity(
                identifier = XbrlIdentifier(
                    scheme = "CIK",
                    value = fyFact.cik
                ),
                segment = XbrlSegment(fyFact.explicitMembers)
            ),
            period = XbrlPeriod(
                instant = fyFact.instant,
                startDate = fyFact.startDate,
                endDate = fyFact.endDate,
            )
        )
    }

    /**
     * An identity context is used to perform context based look up across time periods - thus time period data is not
     * copied as not to affect equality comparison operations
     *
     * The main use is to determine if two different facts across time are indeed referring to the same piece of data
     * for example: Revenue without any dimensional data
     *
     * @param fact the [Fact] to extract an identity context from
     * @return the identity XbrlContext
     */
    private fun toIdentityContext(fact: Fact): XbrlContext {
        return XbrlContext(
            /*
            this is a temporary hijacking of the XbrlContext object to assist identity based lookup
             */
            id = fact.conceptName,
            entity = XbrlEntity(
                identifier = XbrlIdentifier(
                    scheme = "CIK",
                    value = fact.cik
                ),
                segment = XbrlSegment(fact.explicitMembers)
            ),
            /*
            we are using XbrlContext as an identity here, so the period cannot be populated
             */
            period = XbrlPeriod()
        )
    }

}