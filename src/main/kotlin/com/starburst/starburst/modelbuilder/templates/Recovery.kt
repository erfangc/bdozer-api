package com.starburst.starburst.modelbuilder.templates

import com.starburst.starburst.DoubleExtensions.fmtPct
import com.starburst.starburst.edgar.FilingProvider
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareDiluted
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.WeightedAverageNumberOfDilutedSharesOutstanding
import com.starburst.starburst.filingentity.FilingEntityManager
import com.starburst.starburst.modelbuilder.common.AbstractModelBuilder
import com.starburst.starburst.modelbuilder.common.Extensions.businessWaterfall
import com.starburst.starburst.modelbuilder.common.Extensions.fragment
import com.starburst.starburst.modelbuilder.common.ModelResult
import com.starburst.starburst.models.dataclasses.*
import com.starburst.starburst.zacks.dataclasses.Context
import com.starburst.starburst.zacks.modelbuilder.support.SalesEstimateToRevenueConverter
import com.starburst.starburst.zacks.se.ZacksEstimatesService
import java.util.concurrent.Callable

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

        val tradingSymbol = filingEntity.tradingSymbol
        val model = Model(
            name = "Valuation Model - $tradingSymbol",
            symbol = tradingSymbol,
            description = filingEntity.description,
            currentPrice = 22.7,
            beta = 1.86,
            terminalFcfGrowthRate = 0.02
        )

        val incomeStatementItems = statementArcs
            .map { arc ->
                val historicalValue = historicalValue(arc)
                val item = if (arc.calculations.isEmpty()) {
                    Item(
                        name = itemNameFromHref(arc.conceptHref),
                        description = arcLabel(arc),
                        historicalValue = historicalValue,
                        expression = "${historicalValue?.value ?: 0.0}",
                    )
                } else {
                    Item(
                        name = itemNameFromHref(arc.conceptHref),
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
        val zeroGrowthResult = executor.submit(Callable { evaluator.evaluate(zeroRevenueGrowth(finalModel)) })
        val zeroGrowthPrice = zeroGrowthResult.get().targetPrice.coerceAtLeast(0.0)

        val evalResult = evaluator.evaluate(finalModel)

        return ModelResult(
            model = evalResult.model,
            cells = evalResult.cells,

            profitPerShare = profitPerShare(finalModel),
            shareOutstanding = shareOutstanding(finalModel),

            businessWaterfall = businessWaterfall(evalResult),

            currentPrice = model.currentPrice,
            zeroGrowthPrice = zeroGrowthPrice,
            impliedPriceFromGrowth = model.currentPrice - zeroGrowthPrice,

            targetPrice = evalResult.targetPrice,
            discountRate = (model.equityRiskPremium * model.beta) + model.riskFreeRate,
        )
    }

    private fun shareOutstanding(model: Model): Item {
        return model.incomeStatementItems.find { it.name == WeightedAverageNumberOfDilutedSharesOutstanding }
            ?: error("...")
    }

    private fun profitPerShare(model: Model): Item {
        return model.incomeStatementItems.find { it.name == EarningsPerShareDiluted } ?: error("...")
    }

    private fun zeroRevenueGrowth(finalModel: Model): Model {
        val updIs = finalModel.incomeStatementItems.map { item ->
            if (item.name == revenueConceptName) {
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

    private fun processItem(item: Item, model: Model): Item {
        return when {
            item.name == revenueConceptName -> {
                createRevenueItemWithProjection(model, item)
            }
            isEpsItem(item) -> {
                createEpsItem(item)
            }
            isTaxItem(item) -> {
                taxItems(item)
            }
            isOneTime(item) -> {
                item.copy(
                    expression = "0.0",
                    commentaries = Commentary(commentary = "This is a one-time item")
                )
            }
            isCostOperatingCost(item) -> {
                createOperatingCostItem(item)
            }
            else -> {
                item
            }
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
