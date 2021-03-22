package com.starburst.starburst.modelbuilder

import com.starburst.starburst.edgar.FilingProvider
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.starburst.starburst.edgar.factbase.ingestor.dataclasses.Arc
import com.starburst.starburst.edgar.factbase.support.ConceptManager
import com.starburst.starburst.edgar.factbase.support.LabelManager
import com.starburst.starburst.models.EvaluateModelResult
import com.starburst.starburst.models.ModelEvaluator
import com.starburst.starburst.models.Utility.PresentValuePerShare
import com.starburst.starburst.models.dataclasses.HistoricalValue
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.Model
import java.net.URI

class ModelBuilder(
    private val filingProvider: FilingProvider,
    private val factBase: FactBase
) {

    private fun String.fragment(): String = URI(this).fragment
    private val conceptManager = ConceptManager(filingProvider)
    private val labelManager = LabelManager(filingProvider)

    fun arcId(href: String): String {
        return conceptManager.getConceptDefinition(href)?.conceptName ?: href.fragment()
    }

    fun arcLabel(arc: Arc): String {
        val label = labelManager.getLabel(arc.conceptHref.fragment())
        return label?.terseLabel ?: label?.label ?: arc.conceptName
    }

    fun historicalValue(lineItem: Arc): HistoricalValue? {
        val conceptName = lineItem.conceptName
        val cik = filingProvider.cik()
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

    fun buildModel(): EvaluateModelResult {
        val cik = filingProvider.cik()
        val calculations = factBase.calculations(cik)
        val incomeStatement = calculations.incomeStatement

        val lineItemsIdx = incomeStatement.indexOfFirst { it.conceptHref.fragment() == "us-gaap_StatementLineItems" }

        val statementArcs = incomeStatement.subList(
            lineItemsIdx + 1,
            incomeStatement.size
        )

        fun expression(arc: Arc): String {
            val positives = arc
                .calculations
                .filter { it.weight > 0 }
                .joinToString("+") { arcId(it.conceptHref) }

            val negatives = arc
                .calculations
                .filter { it.weight < 0 }
                .joinToString("-") { arcId(it.conceptHref) }

            return if (negatives.isNotEmpty()) {
                "$positives - $negatives"
            } else
                positives
        }

        val incomeStatementItems = statementArcs
            .map { arc ->
                val historicalValue = historicalValue(arc)
                if (arc.calculations.isEmpty()) {
                    Item(
                        name = arcId(arc.conceptHref),
                        description = arcLabel(arc),
                        historicalValue = historicalValue,
                        expression = "${historicalValue?.value ?: 0.0}",
                    )
                } else {
                    Item(
                        name = arcId(arc.conceptHref),
                        description = arcLabel(arc),
                        historicalValue = historicalValue,
                        expression = expression(arc),
                    )
                }
            }

        val model = Model(
            name = "Model",
            incomeStatementItems = incomeStatementItems + Item(name = PresentValuePerShare, expression = "0.0"),
        )
        val evaluator = ModelEvaluator()
        val output = evaluator.evaluate(model)
        return output
    }
}