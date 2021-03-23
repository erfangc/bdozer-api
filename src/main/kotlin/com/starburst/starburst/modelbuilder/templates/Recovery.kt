package com.starburst.starburst.modelbuilder.templates

import com.starburst.starburst.DoubleExtensions.fmtPct
import com.starburst.starburst.edgar.FilingProvider
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareBasic
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareDiluted
import com.starburst.starburst.filingentity.FilingEntityManager
import com.starburst.starburst.modelbuilder.common.Extensions.fragment
import com.starburst.starburst.models.EvaluateModelResult
import com.starburst.starburst.models.dataclasses.Commentary
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.zacks.dataclasses.Context
import com.starburst.starburst.zacks.modelbuilder.support.SalesEstimateToRevenueConverter
import com.starburst.starburst.zacks.se.ZacksEstimatesService

open class Recovery(
    private val filingProvider: FilingProvider,
    private val factBase: FactBase,
    private val zacksEstimatesService: ZacksEstimatesService,
    filingEntityManager: FilingEntityManager,
) : AbstractModelBuilder(filingProvider, factBase, filingEntityManager) {

    override fun buildModel(): EvaluateModelResult {
        val cik = filingProvider.cik()
        val calculations = factBase.calculations(cik)
        val incomeStatement = calculations.incomeStatement

        val lineItemsIdx = incomeStatement.indexOfFirst { it.conceptHref.fragment() == "us-gaap_StatementLineItems" }

        val statementArcs = incomeStatement.subList(
            lineItemsIdx + 1,
            incomeStatement.size
        )

        val model = Model(
            name = "Model",
            symbol = filingEntity.tradingSymbol,
        )
        val incomeStatementItems = statementArcs
            .map { arc ->
                val historicalValue = historicalValue(arc)
                val item = if (arc.calculations.isEmpty()) {
                    Item(
                        name = itemName(arc.conceptHref),
                        description = arcLabel(arc),
                        historicalValue = historicalValue,
                        expression = "${historicalValue?.value ?: 0.0}",
                    )
                } else {
                    Item(
                        name = itemName(arc.conceptHref),
                        description = arcLabel(arc),
                        historicalValue = historicalValue,
                        expression = expression(arc),
                    )
                }
                processItem(item = item, model = model)
            }

        val updatedModel = model.copy(incomeStatementItems = incomeStatementItems)
        return evaluator.evaluate(updatedModel)
    }

    private fun processAsPctOfRevenue(item: Item): Item {
        val ts = timeSeriesVsRevenue(conceptName = item.name)
        val pctOfRev = ts.subList(0, ts.size - 1).map { it.second / it.first }

        return if (pctOfRev.isNotEmpty()) {
            val pct = pctOfRev.average()
            item.copy(
                expression = "$pct * $revenueConceptName",
                commentaries = Commentary(commentary = "Using historical average of ${pct.fmtPct()}, excluding instances when it was >100%")
            )
        } else {
            // perpetuate current
            val pct = ts.last().second / ts.last().first
            item.copy(
                expression = "$pct * $revenueConceptName",
                commentaries = Commentary(
                    commentary = "Insufficient history of clean data, applying the most recent revenue percentage ${pct.fmtPct()} going forward"
                )
            )
        }
    }

    private fun isRevenue(itemName: String) = itemName == revenueConceptName

    private fun processItem(item: Item, model: Model): Item {
        return if (isRevenue(item.name)) {
            processRevenueItem(model, item)
        } else if (item.name == EarningsPerShareBasic || item.name == EarningsPerShareDiluted) {
            processEpsItem(item)
        } else if (isOneTime(item)) {
            item.copy(
                expression = "0.0",
                commentaries = Commentary(commentary = "This is a one-time item")
            )
        } else if (isCostOperatingCost(item)) {
            processOperatingCost(item)
        } else {
            item
        }
    }

    private fun processOperatingCost(item: Item): Item {
        return processAsPctOfRevenue(item)
    }

    private fun processRevenueItem(
        model: Model,
        item: Item
    ): Item {
        val ticker = filingEntity.tradingSymbol ?: error("...")
        val salesEstimates = zacksEstimatesService.getZacksSaleEstimates(ticker)
        val ctx = Context(
            ticker = ticker,
            model = model,
            zacksFundamentalA = emptyList(),
            zacksSalesEstimates = salesEstimates,
        )
        val converter = SalesEstimateToRevenueConverter(ctx = ctx)
        val revenueItem = converter.convert(item.historicalValue?.value ?: error("..."))
        val discrete = revenueItem.discrete
        return revenueItem
            .copy(
                name = item.name,
                discrete = discrete?.copy(
                    formulas = discrete
                        .formulas
                        .mapValues { (_, v) ->
                            (v.toDouble() * 1_000_000.0).toString()
                        }
                )
            )
    }

    private fun processEpsItem(item: Item): Item {
        return when (item.name) {
            EarningsPerShareDiluted -> {
                item.copy(
                    expression = "${USGaapConstants.NetIncomeLoss} / ${USGaapConstants.WeightedAverageNumberOfDilutedSharesOutstanding}"
                )
            }
            EarningsPerShareBasic -> {
                item.copy(
                    expression = "${USGaapConstants.NetIncomeLoss} / ${USGaapConstants.WeightedAverageNumberOfSharesOutstandingBasic}"
                )
            }
            else -> {
                item
            }
        }
    }

    protected fun isCostOperatingCost(item: Item): Boolean =
        conceptDependencies[USGaapConstants.CostsAndExpenses]?.contains(item.name) == true
}