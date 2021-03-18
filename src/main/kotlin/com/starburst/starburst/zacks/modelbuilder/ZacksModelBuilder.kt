package com.starburst.starburst.zacks.modelbuilder

import com.starburst.starburst.models.Utility.DiscountFactor
import com.starburst.starburst.models.Utility.EarningsPerShare
import com.starburst.starburst.models.Utility.NetIncome
import com.starburst.starburst.models.Utility.PresentValuePerShare
import com.starburst.starburst.models.Utility.SharesOutstanding
import com.starburst.starburst.models.Utility.TerminalValuePerShare
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.models.evaluator.ModelEvaluator
import com.starburst.starburst.zacks.dataclasses.BuildModelResponse
import com.starburst.starburst.zacks.dataclasses.Context
import com.starburst.starburst.zacks.dataclasses.KeyInputs
import com.starburst.starburst.zacks.fa.ZacksFundamentalAService
import com.starburst.starburst.zacks.modelbuilder.support.BalanceSheetBuilder
import com.starburst.starburst.zacks.modelbuilder.support.IncomeStatementBuilder
import com.starburst.starburst.zacks.se.ZacksEstimatesService
import org.springframework.stereotype.Service

@Service
class ZacksModelBuilder(
    private val incomeStatementBuilder: IncomeStatementBuilder,
    private val balanceSheetBuilder: BalanceSheetBuilder,
    private val zacksFundamentalAService: ZacksFundamentalAService,
    private val zacksEstimatesService: ZacksEstimatesService,
) {

    private val modelEvaluator = ModelEvaluator()

    /**
     * Build a model using Zacks Fundamental A data
     * for the given ticker - this model will be supplemented by data we ingest from the SEC
     */
    fun buildModel(ticker: String, revenueKeyInputs: KeyInputs? = null): BuildModelResponse {

        val zacksFundamentalA = zacksFundamentalAService.getZacksFundamentalAs(ticker)
        val zacksSalesEstimates = zacksEstimatesService.getZacksSaleEstimates(ticker)

        val skeletonModel = Model(
            symbol = ticker,
        )

        val ctx = Context(
            ticker = ticker,
            model = skeletonModel,
            zacksFundamentalA = zacksFundamentalA,
            zacksSalesEstimates = zacksSalesEstimates
        )

        /*
        Use helper classes to construct IncomeStatement and BalanceSheet [Item]s
         */
        val incomeStatement = incomeStatementBuilder.incomeStatementItems(ctx, revenueKeyInputs)
        val balanceSheet = balanceSheetBuilder.balanceSheetItems(ctx)

        /*
        Attach the newly built income statement/balance-sheet items
         */
        val modelWithItems = skeletonModel.copy(
            incomeStatementItems = incomeStatement.items,
            balanceSheetItems = balanceSheet.items,
        )

        val otherItems = deriveOtherItems(modelWithItems)
        val finalModel = modelWithItems.copy(otherItems = otherItems)

        val evaluateModelResult = modelEvaluator.evaluate(finalModel)

        return BuildModelResponse(
            evaluateModelResult = evaluateModelResult,
            incomeStatement = incomeStatement,
            balanceSheet = balanceSheet,
        )

    }

    private fun deriveOtherItems(model: Model): List<Item> {
        val periods = model.periods
        val discountRate = (model.equityRiskPremium * model.beta) + model.riskFreeRate
        val terminalPeMultiple = 1.0 / (discountRate - model.terminalFcfGrowthRate)
        return listOf(
            Item(
                name = DiscountFactor,
                expression = "1 / (1.0 + $discountRate)^period"
            ),
            Item(
                name = EarningsPerShare,
                expression = "$NetIncome / $SharesOutstanding"
            ),
            Item(
                name = TerminalValuePerShare,
                expression = "if(period=$periods,$EarningsPerShare * ${terminalPeMultiple},0.0)"
            ),
            Item(
                name = PresentValuePerShare,
                expression = "$DiscountFactor * ($EarningsPerShare + $TerminalValuePerShare)"
            )
        )
    }

}
