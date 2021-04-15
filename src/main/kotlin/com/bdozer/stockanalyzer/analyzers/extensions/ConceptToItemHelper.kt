package com.bdozer.stockanalyzer.analyzers.extensions

import com.bdozer.edgar.dataclasses.Concept
import com.bdozer.edgar.factbase.FactExtensions.filterForDimensions
import com.bdozer.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.bdozer.edgar.factbase.ingestor.dataclasses.Arc
import com.bdozer.extensions.DoubleExtensions.orZero
import com.bdozer.models.dataclasses.HistoricalValue
import com.bdozer.models.dataclasses.Item
import com.bdozer.stockanalyzer.analyzers.AbstractStockAnalyzer
import com.bdozer.stockanalyzer.analyzers.extensions.General.fragment

object ConceptToItemHelper {

    /**
     * For an Arc with dependent calculations
     * create a String based formula based on the weight
     * and concept defined in those calculations
     */
    fun AbstractStockAnalyzer.expression(arc: Arc): String {
        val positives = arc
            .calculations
            .filter { it.weight > 0 }
            .joinToString("+") { conceptHrefToItemName(it.conceptHref) }

        val negatives = arc
            .calculations
            .filter { it.weight < 0 }
            .joinToString("-") { conceptHrefToItemName(it.conceptHref) }

        return if (negatives.isNotEmpty()) {
            "$positives - $negatives"
        } else {
            positives
        }
    }

    /**
     * Determines the "name" of an [Item] based on the
     * href of a concept
     */
    fun AbstractStockAnalyzer.conceptHrefToItemName(conceptHref: String): String {
        return conceptManager
            .getConcept(conceptHref)
            ?.conceptName ?: conceptHref.fragment()
    }

    /**
     * Find the label of an [Arc]
     */
    fun AbstractStockAnalyzer.conceptLabel(conceptHref: String): String {
        val conceptId = conceptHref.fragment()
        val label = labelManager.getLabel(conceptId)
        return label?.terseLabel ?: label?.label ?: conceptId
    }

    /**
     * Find the [HistoricalValue] of an [Arc] (most recent annual)
     */
    fun AbstractStockAnalyzer.historicalValue(concept: Concept): HistoricalValue? {
        val conceptName = concept.conceptName
        return historicalValue(conceptName)
    }

    /**
     * Find the [HistoricalValue] of an [Arc] (most recent annual)
     */
    fun AbstractStockAnalyzer.historicalValue(conceptName: String): HistoricalValue? {

        val facts = factBase.getFacts(cik, DocumentFiscalPeriodFocus.FY, conceptName).sortedByDescending { it.documentPeriodEndDate }
        val fact = facts.find { fact -> fact.explicitMembers.isEmpty() }

        if (fact != null) {
            return HistoricalValue(
                factId = fact._id,
                documentFiscalPeriodFocus = fact.documentFiscalPeriodFocus.toString(),
                documentPeriodEndDate = fact.documentPeriodEndDate.toString(),
                value = fact.doubleValue,
                startDate = fact.startDate.toString(),
                endDate = fact.endDate.toString(),
                instant = fact.instant.toString(),
            )
        } else {
            /*
            now we return facts as decomposed by dimensions declared
            on the income statement
             */
            val matchingFacts = (facts
                .groupBy { it.documentPeriodEndDate }
                .entries.maxByOrNull { it.key } ?: return null)
                .value
                .filterForDimensions(dimensions)

            val firstFact = matchingFacts.firstOrNull() ?: return null
            val factIds = matchingFacts.map { it._id }
            val value = matchingFacts.sumByDouble { it.doubleValue.orZero() }

            return HistoricalValue(
                factIds = factIds,
                documentFiscalPeriodFocus = firstFact.documentFiscalPeriodFocus.toString(),
                documentPeriodEndDate = firstFact.documentPeriodEndDate.toString(),
                value = value,
                startDate = firstFact.startDate.toString(),
                endDate = firstFact.endDate.toString(),
                instant = firstFact.instant.toString(),
            )
        }

    }

}