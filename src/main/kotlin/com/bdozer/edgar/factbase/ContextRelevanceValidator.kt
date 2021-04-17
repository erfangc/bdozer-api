package com.bdozer.edgar.factbase

import com.bdozer.edgar.dataclasses.XbrlContext
import com.bdozer.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.bdozer.edgar.factbase.ingestor.InstanceDocumentExtensions.documentFiscalPeriodFocus
import com.bdozer.edgar.factbase.ingestor.InstanceDocumentExtensions.documentFiscalYearFocus
import com.bdozer.edgar.factbase.ingestor.InstanceDocumentExtensions.documentPeriodEndDate
import com.bdozer.xml.XmlElement
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class ContextRelevanceValidator(filingProvider: FilingProvider) {

    val factsParser = filingProvider.factsParser()
    val instanceDocument = filingProvider.instanceDocument()
    val documentPeriodEndDate = instanceDocument.documentPeriodEndDate()
    val documentFiscalPeriodFocus = instanceDocument.documentFiscalPeriodFocus()

    private val contexts = factsParser
        .contexts
        .values
        .filter { ctx -> ctx.period.endDate == documentPeriodEndDate }

    private val idealDaysInPeriod = if (documentFiscalPeriodFocus == DocumentFiscalPeriodFocus.FY) 365 else 90

    val minDelta = contexts.minOf { ctx ->
        val period = ctx.period
        val startDate = period.startDate
        val endDate = period.endDate
        val daysInPeriod = ChronoUnit.DAYS.between(startDate, endDate)
        abs(idealDaysInPeriod - daysInPeriod)
    }

    private val dateRelevantContext = contexts.filter { ctx ->
        val period = ctx.period
        val startDate = period.startDate
        val endDate = period.endDate
        val unit = ChronoUnit.DAYS.between(startDate, endDate)
        val delta = abs(idealDaysInPeriod - unit)
        delta == minDelta
    }.toSet()


    fun isContextRelevant(context: XbrlContext): Boolean {
        return dateRelevantContext.contains(context) || context.period.instant == documentPeriodEndDate
    }
}