package com.starburst.starburst.modelbuilder.common.extensions

import com.starburst.starburst.edgar.dataclasses.Concept
import com.starburst.starburst.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.starburst.starburst.edgar.factbase.ingestor.dataclasses.Arc
import com.starburst.starburst.modelbuilder.common.AbstractStockAnalyzer
import com.starburst.starburst.modelbuilder.common.GeneralExtensions.fragment
import com.starburst.starburst.models.dataclasses.HistoricalValue
import com.starburst.starburst.models.dataclasses.Item

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
        val fact = factBase.getFacts(
            cik,
            DocumentFiscalPeriodFocus.FY,
            conceptName
        ).find { fact -> fact.explicitMembers.isEmpty() } ?: return null

        return HistoricalValue(
            factId = fact._id,
            documentFiscalPeriodFocus = fact.documentFiscalPeriodFocus.toString(),
            documentPeriodEndDate = fact.documentPeriodEndDate.toString(),
            value = fact.doubleValue,
            startDate = fact.startDate.toString(),
            endDate = fact.endDate.toString(),
            instant = fact.instant.toString(),
        )
    }

}