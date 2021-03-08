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
class Q4FactFiller(
    mongoClient: MongoClient
) {

    private val col = mongoClient
        .getDatabase("starburst")
        .getCollection<Fact>()
    private val log = LoggerFactory.getLogger(Q4FactFiller::class.java)

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
            .groupBy { fact -> factToContext(fact) }
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

            val context = factToContext(fyFact)

            if (fyFact.doubleValue != null) {
                /*
                derive q4 value
                 */
                val valuesByQ = quarters[context] ?: emptyMap()

                try {
                    // try to sum over the past 3 quarter's data and then subtract them from the 10-K
                    val value = fyFact.doubleValue - listOf("Q1", "Q2", "Q3").sumByDouble { quarter ->
                        valuesByQ[quarter]?.doubleValue
                            ?: error("unable to find $quarter for FY element ${fyFact.elementName}")
                    }
                    fyFact.copy(
                        _id = generateId(fyFact),
                        formType = "10-Q",
                        documentFiscalPeriodFocus = "Q4",
                        period = context.period,
                        stringValue = value.toString(),
                        doubleValue = value,
                        lastUpdated = Instant.now().toString(),
                    )
                } catch (e: Exception) {
                    log.error("error summing past 3 quarter's data", e)
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
                    period = context.period,
                    stringValue = fyFact.stringValue,
                    lastUpdated = Instant.now().toString(),
                )
            }
        }
        q4Facts.forEach { q4Fact ->
            col.save(q4Fact)
        }
    }

    private fun generateId(fyFact: Fact): String {
        //
        // copy the context of fyFact but change the start date
        //
        val fyCtx = factToContext(fyFact)
        val startDate = fyCtx.period.endDate?.let { endDate ->
            YearQuarter.from(endDate).atDay(1)
        }
        val q4Ctx = fyCtx.copy(
            period = XbrlPeriod(
                startDate = startDate,
            )
        )
        return FactIdGenerator()
            .generateId(
                fyFact.elementName,
                q4Ctx,
                fyFact.documentPeriodEndDate
            )
    }

    /**
     * This method is used to create an `identity` context
     * that can be used for lookup across periods (thus dates are not copied from the passed in [fact])
     */
    private fun factToContext(fact: Fact): XbrlContext {
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