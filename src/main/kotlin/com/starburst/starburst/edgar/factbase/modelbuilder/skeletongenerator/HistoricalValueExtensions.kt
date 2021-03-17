package com.starburst.starburst.edgar.factbase.modelbuilder.skeletongenerator

import com.starburst.starburst.edgar.dataclasses.XbrlExplicitMember
import com.starburst.starburst.edgar.factbase.DocumentFiscalPeriodFocus
import com.starburst.starburst.models.dataclasses.HistoricalValue
import com.starburst.starburst.models.dataclasses.HistoricalValues
import kotlin.math.min

object HistoricalValueExtensions {

    fun SkeletonGenerator.allHistoricalValues(
        elementName: String,
        explicitMembers: List<XbrlExplicitMember> = emptyList()
    ): HistoricalValues {
        return HistoricalValues(
            fiscalYear = fiscalYearHistoricalValues(elementName, explicitMembers),
            quarterly = quarterlyHistoricalValues(elementName, explicitMembers),
            ltm = ltm(elementName, explicitMembers),
        )
    }

    fun SkeletonGenerator.latestHistoricalValue(
        elementName: String,
        explicitMembers: List<XbrlExplicitMember> = emptyList()
    ): HistoricalValue? {
        val latest = facts
            .filter { it.conceptName == elementName && it.explicitMembers == explicitMembers }
            .maxByOrNull { it.documentPeriodEndDate }
        when {
            latest == null -> {
                return null
            }
            latest.documentFiscalPeriodFocus == DocumentFiscalPeriodFocus.FY -> {
                return HistoricalValue(
                    factId = latest._id,
                    documentFiscalYearFocus = latest.documentFiscalYearFocus,
                    documentFiscalPeriodFocus = latest.documentFiscalPeriodFocus.toString(),
                    documentPeriodEndDate = latest.documentPeriodEndDate.toString(),
                    value = latest.doubleValue,
                    startDate = latest.startDate?.toString(),
                    endDate = latest.endDate?.toString(),
                    instant = latest.instant?.toString(),
                )
            }
            else -> {
                // return the LTM figure
                return ltm(elementName, explicitMembers)
            }
        }
    }

    fun SkeletonGenerator.ltm(
        elementName: String,
        explicitMembers: List<XbrlExplicitMember> = emptyList()
    ): HistoricalValue? {
        /*
        1 - figure out the latest date for which there is data
         */
        val latestFact = facts
            .filter { fact -> fact.conceptName == elementName && fact.explicitMembers == explicitMembers }
            .maxByOrNull { fact ->
                fact.documentPeriodEndDate
            } ?: return null

        // instant facts do not require summation
        if (
            latestFact.documentFiscalPeriodFocus == DocumentFiscalPeriodFocus.FY
            ||
            latestFact.instant != null
        ) {
            /*
            return the latest FY figure
             */
            return HistoricalValue(
                factId = latestFact._id,
                documentFiscalYearFocus = latestFact.documentFiscalYearFocus,
                documentFiscalPeriodFocus = latestFact.documentFiscalPeriodFocus.toString(),
                documentPeriodEndDate = latestFact.documentPeriodEndDate.toString(),
                value = latestFact.doubleValue,
                startDate = latestFact.startDate?.toString(),
                endDate = latestFact.endDate?.toString(),
                instant = latestFact.instant?.toString(),
            )

        } else {
            /*
            sum up the previous 4 quarters
             */
            val quarters = this.quarterlyHistoricalValues(elementName, explicitMembers)
            val ltm = quarters
                .sortedByDescending { it.documentPeriodEndDate }
                .subList(0, min(4, quarters.size))
                .sumByDouble { it.value ?: 0.0 }

            return HistoricalValue(
                factId = latestFact._id,
                documentFiscalYearFocus = latestFact.documentFiscalYearFocus,
                documentFiscalPeriodFocus = latestFact.documentFiscalPeriodFocus.toString(),
                documentPeriodEndDate = latestFact.documentPeriodEndDate.toString(),
                value = ltm,
                startDate = latestFact.startDate?.toString(),
                endDate = latestFact.endDate?.toString(),
                instant = latestFact.instant?.toString(),
            )
        }
    }

    fun SkeletonGenerator.fiscalYearHistoricalValues(
        elementName: String,
        explicitMembers: List<XbrlExplicitMember> = emptyList()
    ): List<HistoricalValue> {
        return facts
            .filter { fact ->
                fact.explicitMembers == explicitMembers
                        && fact.conceptName == elementName
                        && fact.documentFiscalPeriodFocus == DocumentFiscalPeriodFocus.FY
            }
            .map { fact ->
                HistoricalValue(
                    factId = fact._id,
                    documentFiscalYearFocus = fact.documentFiscalYearFocus,
                    documentFiscalPeriodFocus = fact.documentFiscalPeriodFocus.toString(),
                    documentPeriodEndDate = fact.documentPeriodEndDate.toString(),
                    value = fact.doubleValue,
                    startDate = fact.startDate?.toString(),
                    endDate = fact.endDate?.toString(),
                    instant = fact.instant?.toString(),
                )
            }
            .sortedByDescending { fact ->
                fact.documentPeriodEndDate
            }
    }

    fun SkeletonGenerator.quarterlyHistoricalValues(
        elementName: String,
        explicitMembers: List<XbrlExplicitMember> = emptyList()
    ): List<HistoricalValue> {
        val filter = facts
            .filter { fact ->
                fact.explicitMembers == explicitMembers
                        && fact.conceptName == elementName
                        && fact.documentFiscalPeriodFocus.name.startsWith("Q")
            }
        return filter
            .map { fact ->
                HistoricalValue(
                    factId = fact._id,
                    documentFiscalYearFocus = fact.documentFiscalYearFocus,
                    documentFiscalPeriodFocus = fact.documentFiscalPeriodFocus.toString(),
                    documentPeriodEndDate = fact.documentPeriodEndDate.toString(),
                    value = fact.doubleValue,
                    startDate = fact.startDate?.toString(),
                    endDate = fact.endDate?.toString(),
                    instant = fact.instant?.toString(),
                )
            }
            .sortedByDescending { fact ->
                fact.documentPeriodEndDate
            }
    }

}