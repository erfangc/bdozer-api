package com.starburst.starburst.edgar.factbase.ingestor.q4

import com.mongodb.client.MongoClient
import com.starburst.starburst.edgar.dataclasses.*
import com.starburst.starburst.edgar.factbase.ingestor.FactIdGenerator
import org.litote.kmongo.*
import org.slf4j.LoggerFactory
import org.threeten.extra.YearQuarter
import java.time.Instant

/**
 * After parsing 3 10-Qs per fiscal year and 1 10-K - we are now
 * missing the pure Q4 figures - for that we must actually retrieve
 * the 10-K figure and the past 3 10-Qs
 */
class Q4FactFinder(
    mongoClient: MongoClient
) {

    private val col = mongoClient
        .getDatabase("starburst")
        .getCollection<Fact>()
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
                Fact::documentFiscalPeriodFocus ne "Q4"
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
                val valuesByQ = quarters[fyContext] ?: emptyMap()

                try {
                    //
                    // try to sum over the past 3 quarter's data and then subtract them from the 10-K
                    //
                    val value = fyFact.doubleValue - listOf("Q1", "Q2", "Q3").sumByDouble { quarter ->
                        valuesByQ[quarter]?.doubleValue
                            ?: error("unable to find $quarter for FY element ${fyFact.elementName}")
                    }
                    fyFact.copy(
                        _id = generateId(fyFact),
                        formType = "10-Q",
                        documentFiscalPeriodFocus = "Q4",
                        period = q4Context.period,
                        stringValue = value.toString(),
                        doubleValue = value,
                        lastUpdated = Instant.now().toString(),
                    )
                } catch (e: Exception) {
                    log.error("error summing past 3 quarter's data, error, ${e.message}")
                    null
                }
            } else {
                /*
                no need to derive Q4 values
                 */
                fyFact.copy(
                    _id = generateId(fyFact),
                    formType = "10-Q",
                    documentFiscalPeriodFocus = "Q4",
                    period = q4Context.period,
                    lastUpdated = Instant.now().toString(),
                )
            }
        }
        log.info("Found ${q4Facts.size} Q4 facts, saving them now")
        q4Facts.forEach { q4Fact ->
            col.save(q4Fact)
        }
        log.info("Saved ${q4Facts.size} Q4 facts")
    }

    private fun generateId(fyFact: Fact): String {
        //
        // copy the context of fyFact but change the start date
        //
        val fyCtx = toFyContext(fyFact)
        return FactIdGenerator()
            .generateId(
                fyFact.elementName,
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
        return fyCtx.copy(
            period = XbrlPeriod(
                startDate = startDate,
            )
        )
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
            period = fyFact.period
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
            id = fact.elementName,
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