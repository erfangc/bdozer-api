package com.starburst.starburst.modelbuilder.templates

import com.starburst.starburst.DoubleExtensions.fmtPct
import com.starburst.starburst.edgar.FilingProvider
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareBasic
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareDiluted
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.IncomeTaxExpenseBenefit
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.NetIncomeLoss
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.WeightedAverageNumberOfDilutedSharesOutstanding
import com.starburst.starburst.filingentity.FilingEntityManager
import com.starburst.starburst.modelbuilder.common.Extensions.fragment
import com.starburst.starburst.models.dataclasses.*
import com.starburst.starburst.zacks.dataclasses.Context
import com.starburst.starburst.zacks.modelbuilder.support.SalesEstimateToRevenueConverter
import com.starburst.starburst.zacks.se.ZacksEstimatesService

class Recovery(
    private val filingProvider: FilingProvider,
    private val factBase: FactBase,
    private val zacksEstimatesService: ZacksEstimatesService,
    filingEntityManager: FilingEntityManager,
) : AbstractModelBuilder(filingProvider, factBase, filingEntityManager) {

    override fun buildModel(): ModelResult {
        val cik = filingProvider.cik()
        val calculations = factBase.calculations(cik)
        val incomeStatement = calculations.incomeStatement

        val lineItemsIdx = incomeStatement.indexOfFirst {
            it.conceptHref.fragment() == "us-gaap_StatementLineItems"
        }

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

        val modelWithIncomeStatement = model.copy(
            incomeStatementItems = incomeStatementItems,
        )

        val finalModel = modelWithIncomeStatement.copy(
            otherItems = deriveOtherItems(modelWithIncomeStatement)
        )

        /*
        try a version of this where revenue remains constant
         */
        val zeroGrowthResult = evaluator.evaluate(zeroRevenueGrowth(finalModel))
        val zeroGrowthPrice = zeroGrowthResult.targetPrice.coerceAtLeast(0.0)

        val evalResult = evaluator.evaluate(finalModel)
        val currentPrice = 23.83

        return ModelResult(
            cells = evalResult.cells,
            model = evalResult.model,

            revenue = revenue(finalModel),
            categorizedExpenses = categorizedExpenses(finalModel),
            profit = profit(finalModel),
            profitPerShare = profitPerShare(finalModel),
            shareOutstanding = shareOutstanding(finalModel),

            currentPrice = currentPrice,
            zeroGrowthPrice = zeroGrowthPrice,
            impliedPriceFromGrowth = currentPrice - zeroGrowthPrice,

            targetPrice = evalResult.targetPrice,
        )
    }

    private fun isTaxItem(item: Item): Boolean = item.name == IncomeTaxExpenseBenefit

    private fun taxItems(item: Item): Item {
        return item.copy(
            expression = "$IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest*0.12"
        )
    }

    private fun shareOutstanding(model: Model): Item {
        return model.incomeStatementItems.find { it.name == WeightedAverageNumberOfDilutedSharesOutstanding }
            ?: error("...")
    }

    private fun profitPerShare(model: Model): Item {
        return model.incomeStatementItems.find { it.name == EarningsPerShareDiluted } ?: error("...")
    }

    private fun profit(model: Model): Item {
        return model.incomeStatementItems.find { it.name == NetIncomeLoss } ?: error("...")
    }

    private fun categorizedExpenses(model: Model): List<Item> {

        val concepts = conceptDependencies[NetIncomeLoss]
            ?.filter { !isRevenue(it) }
            ?.mapNotNull { conceptName -> model.incomeStatementItems.find { it.name == conceptName } }
            ?.sortedByDescending { it.historicalValue?.value ?: 0.0 }
            ?: error("...")

        val conceptSize = concepts.size
        val cutoff = 5

        return if (conceptSize > cutoff) {
            val others = concepts.subList(cutoff, concepts.size)
            val subList = concepts.subList(0, cutoff)
            subList + Item(
                name = "Others",
                historicalValue = HistoricalValue(
                    value = others.sumByDouble { it.historicalValue?.value ?: 0.0 }
                )
            )
        } else {
            concepts
        }
    }

    private fun revenue(model: Model): Item {
        return model.incomeStatementItems.find { isRevenue(it.name) } ?: error("...")
    }

    private fun zeroRevenueGrowth(finalModel: Model): Model {
        val updIs = finalModel.incomeStatementItems.map { item ->
            if (isRevenue(item.name)) {
                item.copy(expression = item.historicalValue?.value.toString(), type = ItemType.Custom)
            } else {
                item
            }
        }
        return finalModel.copy(incomeStatementItems = updIs)
    }

    private fun itemAsPercentOfRevenue(item: Item): Item {
        val ts = timeSeriesVsRevenue(conceptName = item.name)
        val pctOfRev = ts.subList(0, ts.size - 1).map { it.second / it.first }

        return if (pctOfRev.isNotEmpty()) {
            val pct = pctOfRev.average()
            item.copy(
                expression = "$pct * $revenueConceptName",
                commentaries = Commentary(
                    commentary = "Using historical average of ${pct.fmtPct()}, excluding instances when it was >100%"
                )
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
            createRevenueItemWithProjection(model, item)
        } else if (item.name == EarningsPerShareBasic || item.name == EarningsPerShareDiluted) {
            createEpsItem(item)
        } else if (isTaxItem(item)) {
            taxItems(item)
        } else if (isOneTime(item)) {
            item.copy(
                expression = "0.0",
                commentaries = Commentary(commentary = "This is a one-time item")
            )
        } else if (isCostOperatingCost(item)) {
            createOperatingCostItem(item)
        } else {
            item
        }
    }

    private fun createOperatingCostItem(item: Item): Item {
        return itemAsPercentOfRevenue(item)
    }

    private fun createRevenueItemWithProjection(model: Model, item: Item): Item {
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

    private fun isCostOperatingCost(item: Item): Boolean =
        conceptDependencies[USGaapConstants.CostsAndExpenses]?.contains(item.name) == true
}
