package com.starburst.starburst.modelbuilder.templates

import com.starburst.starburst.DoubleExtensions.fmtPct
import com.starburst.starburst.DoubleExtensions.orZero
import com.starburst.starburst.edgar.FilingProvider
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareDiluted
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.WeightedAverageNumberOfDilutedSharesOutstanding
import com.starburst.starburst.filingentity.dataclasses.FilingEntity
import com.starburst.starburst.modelbuilder.common.AbstractModelBuilder
import com.starburst.starburst.modelbuilder.common.Extensions.businessWaterfall
import com.starburst.starburst.modelbuilder.common.Extensions.fragment
import com.starburst.starburst.modelbuilder.common.StockAnalysis
import com.starburst.starburst.models.EvaluateModelResult
import com.starburst.starburst.models.dataclasses.Commentary
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.zacks.dataclasses.Context
import com.starburst.starburst.zacks.modelbuilder.support.SalesEstimateToRevenueConverter
import com.starburst.starburst.zacks.se.ZacksEstimatesService
import java.util.concurrent.Callable
import kotlin.math.pow

class EarningsRecoveryAnalyzer(
    private val zacksEstimatesService: ZacksEstimatesService,
    filingProvider: FilingProvider,
    factBase: FactBase,
    filingEntity: FilingEntity,
) : AbstractModelBuilder(filingProvider, factBase, filingEntity) {

    override fun analyze(): StockAnalysis {
        val lineItemsIdx = incomeStatement.indexOfFirst {
            it.conceptHref.fragment() == "us-gaap_StatementLineItems"
        }

        val statementArcs = incomeStatement.subList(
            lineItemsIdx + 1,
            incomeStatement.size
        )

        val model = Model(
            name = filingEntity.name,
            symbol = tradingSymbol,
            description = filingEntity.description,
            beta = 1.86,
            terminalGrowthRate = 0.02
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
        val revenueCAGR = revenueCAGR(evalResult)

        return StockAnalysis(
            _id = cik,

            model = evalResult.model,
            cells = evalResult.cells,

            profitPerShare = profitPerShare(finalModel),
            shareOutstanding = shareOutstanding(finalModel),

            businessWaterfall = businessWaterfall(evalResult),

            zeroGrowthPrice = zeroGrowthPrice,

            targetPrice = evalResult.targetPrice,
            discountRate = (model.equityRiskPremium * model.beta) + model.riskFreeRate,

            revenueCAGR = revenueCAGR,

            )
    }

    private fun revenueCAGR(evalResult: EvaluateModelResult): Double {
        val revenues = evalResult
            .cells
            .filter { cell -> cell.item.name == revenueConceptName }
        return (revenues.last().value.orZero() / revenues.first().value.orZero()).pow(1.0 / revenues.size) - 1
    }

    protected fun shareOutstanding(model: Model): Item {
        return model.incomeStatementItems.find { it.name == WeightedAverageNumberOfDilutedSharesOutstanding }
            ?: error("...")
    }

    private fun profitPerShare(model: Model): Item {
        return model.incomeStatementItems.find { it.name == EarningsPerShareDiluted } ?: error("...")
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
        conceptDependencies[USGaapConstants.CostsAndExpenses]?.map { it.conceptName }?.contains(item.name) == true
}
