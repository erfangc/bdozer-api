package com.starburst.starburst.edgar.factbase.ingestor.q4

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.ReplaceOptions
import com.starburst.starburst.edgar.dataclasses.*
import com.starburst.starburst.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.ingestor.support.FactIdGenerator
import org.litote.kmongo.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.threeten.extra.YearQuarter
import java.time.Instant

/**
 * After parsing 3 10-Qs per fiscal year and 1 10-K - we are now
 * missing the pure Q4 figures - for that we must actually retrieve
 * the 10-K figure and the past 3 10-Qs
 */
@Service
class Q4FactFinder(
    mongoDatabase: MongoDatabase
) {

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

        val errors = mutableListOf<String>()
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
                derive q4 value
                 */
                val valuesByQ = quarters[identityContext] ?: emptyMap()

                try {
                    /*
                    try to sum over the past 3 quarter's data and then subtract them from the 10-K
                     */
                    val value = fyFact.doubleValue - listOf(
                        DocumentFiscalPeriodFocus.Q1,
                        DocumentFiscalPeriodFocus.Q2,
                        DocumentFiscalPeriodFocus.Q3,
                    ).sumByDouble { quarter ->
                        valuesByQ[quarter]
                            ?.doubleValue
                            ?: error("unable to find $quarter for FY element ${fyFact.conceptName}")
                    }

                    fyFact.copy(
                        _id = generateId(fyFact),
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
                    errors.add("${fyFact.conceptName} ${fyContext.entity.segment?.explicitMembers?.joinToString(",")}")
                    null
                }
            } else {
                /*
                no need to derive Q4 values
                 */
                fyFact.copy(
                    _id = generateId(fyFact),
                    formType = "10-Q",
                    documentFiscalPeriodFocus = DocumentFiscalPeriodFocus.Q4,
                    startDate = q4Context.period.startDate,
                    endDate = q4Context.period.endDate,
                    instant = q4Context.period.instant,
                    lastUpdated = Instant.now().toString(),
                )
            }
        }
        log.info("Unable to infer Q4 items for fiscalYear=$fiscalYear for ${errors.joinToString(";")}")
        q4Facts.chunked(55).forEach { chunk ->
            val bulk = chunk.map { replaceOne(Fact::_id eq it._id, it, ReplaceOptions().upsert(true)) }
            col.bulkWrite(bulk)
        }
        log.info("Saved ${q4Facts.size} Q4 facts")
    }

    private fun generateId(fyFact: Fact): String {
        /*
        copy the context of fyFact but change the start date
         */
        val fyCtx = toFyContext(fyFact)
        return FactIdGenerator()
            .generateId(
                fyFact.conceptName,
                q4Context(fyCtx),
                fyFact.documentPeriodEndDate
            )
    }

    /**
     * Create the theoretical Q4 context:
     *
     * if the fyContext has an endDate and not an instant, then we need to figure out the start date
     * otherwise nothing about this context changes
     */
    private fun q4Context(fyCtx: XbrlContext): XbrlContext {
        val startDate = fyCtx.period.endDate?.let { endDate ->
            YearQuarter.from(endDate).atDay(1)
        }
        return fyCtx.copy(period = XbrlPeriod(startDate = startDate))
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
     */
    private fun toIdentityContext(fact: Fact): XbrlContext {
        return XbrlContext(
            // this is a temporary hijacking of the XbrlContext object to assist identity based lookup
            id = fact.conceptName,
            entity = XbrlEntity(
                identifier = XbrlIdentifier(
                    scheme = "CIK",
                    value = fact.cik
                ),
                segment = XbrlSegment(fact.explicitMembers)
            ),
            // we are using XbrlContext as an identity here, so the period cannot be populated
            period = XbrlPeriod()
        )
    }

}