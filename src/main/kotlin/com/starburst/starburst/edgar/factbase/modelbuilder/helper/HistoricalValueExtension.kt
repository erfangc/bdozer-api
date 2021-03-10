package com.starburst.starburst.edgar.factbase.modelbuilder.helper

import com.starburst.starburst.edgar.dataclasses.XbrlExplicitMember
import com.starburst.starburst.models.HistoricalValue
import com.starburst.starburst.models.HistoricalValues
import kotlin.math.min

object HistoricalValueExtension {

    fun ModelBuilderHelper.allHistoricalValues(
        elementName: String,
        explicitMembers: List<XbrlExplicitMember> = emptyList()
    ): HistoricalValues {
        return HistoricalValues(
            fiscalYear = fiscalYearHistoricalValues(elementName, explicitMembers),
            quarterly = quarterlyHistoricalValues(elementName, explicitMembers),
            ltm = ltm(elementName, explicitMembers),
        )
    }

    fun ModelBuilderHelper.latestHistoricalValue(
        elementName: String,
        explicitMembers: List<XbrlExplicitMember> = emptyList()
    ): HistoricalValue? {
        val latest = facts
            .filter { it.elementName == elementName && it.explicitMembers == explicitMembers }
            .maxByOrNull { it.documentPeriodEndDate }
        when {
            latest == null -> {
                return null
            }
            latest.documentFiscalPeriodFocus == "FY" -> {
                return HistoricalValue(
                    factId = latest._id,
                    documentFiscalYearFocus = latest.documentFiscalYearFocus,
                    documentFiscalPeriodFocus = latest.documentFiscalPeriodFocus,
                    documentPeriodEndDate = latest.documentPeriodEndDate,
                    value = latest.doubleValue,
                    startDate = latest.period.startDate?.toString(),
                    endDate = latest.period.endDate?.toString(),
                    instant = latest.period.instant?.toString(),
                )
            }
            else -> {
                // return the LTM figure
                return ltm(elementName, explicitMembers)
            }
        }
    }

    fun ModelBuilderHelper.ltm(
        elementName: String,
        explicitMembers: List<XbrlExplicitMember> = emptyList()
    ): HistoricalValue? {
        /*
        1 - figure out the latest date for which there is data
         */
        val latestFact = facts
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

    fun ModelBuilderHelper.fiscalYearHistoricalValues(
        elementName: String,
        explicitMembers: List<XbrlExplicitMember> = emptyList()
    ): List<HistoricalValue> {
        return facts
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

    fun ModelBuilderHelper.quarterlyHistoricalValues(
        elementName: String,
        explicitMembers: List<XbrlExplicitMember> = emptyList()
    ): List<HistoricalValue> {
        val filter = facts
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

}