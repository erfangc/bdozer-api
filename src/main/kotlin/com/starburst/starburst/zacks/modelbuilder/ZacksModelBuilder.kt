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
import com.starburst.starburst.models.translator.CellGenerator
import com.starburst.starburst.zacks.fa.ZacksFundamentalA
import com.starburst.starburst.zacks.fa.ZacksFundamentalAService
import com.starburst.starburst.zacks.modelbuilder.keyinputs.KeyInputs
import com.starburst.starburst.zacks.modelbuilder.support.BalanceSheetItemsBuilder
import com.starburst.starburst.zacks.modelbuilder.support.IncomeStatementItemsBuilder
import org.springframework.stereotype.Service

@Service
class ZacksModelBuilder(
    private val incomeStatementItemsBuilder: IncomeStatementItemsBuilder,
    private val balanceSheetItemsBuilder: BalanceSheetItemsBuilder,
    private val zacksFundamentalAService: ZacksFundamentalAService,
) {

    private val modelEvaluator = ModelEvaluator()

    /**
     * Build a model using Zacks Fundamental A data
     * for the given ticker - this model will be supplemented by data we ingest from the SEC
     */
    fun buildModel(ticker: String, keyInputs: KeyInputs? = null): BuildModelResponse {
        val fundamentalAs = findZacksFundamentalA(ticker)
        val latestFundamentalA = fundamentalAs
            .filter { it.per_type == "A" }
            .maxByOrNull { it.per_end_date!! }
            ?: error("unable to find an annual fundamental for $ticker, found total fundamentals ${fundamentalAs.size}")

        val skeletonModel = Model(
            symbol = ticker,
            name = latestFundamentalA.comp_name ?: "N/A",
        )

        val incomeStatementItems =
            incomeStatementItemsBuilder.incomeStatementItems(skeletonModel, latestFundamentalA, keyInputs)
        val balanceSheetItems = balanceSheetItemsBuilder.balanceSheetItems(skeletonModel, latestFundamentalA)

        val modelItemized = skeletonModel.copy(
            incomeStatementItems = incomeStatementItems,
            balanceSheetItems = balanceSheetItems,
            cashFlowStatementItems = listOf(),
            otherItems = listOf(),
        )

        val finalModel = modelItemized.copy(
            otherItems = deriveOtherItems(modelItemized)
        )

        val evaluateModelResult = modelEvaluator.evaluate(finalModel)
        val bytes = CellGenerator.exportToXls(finalModel, evaluateModelResult.cells)

        return BuildModelResponse(
            model = evaluateModelResult.model,
            targetPrice = evaluateModelResult.targetPrice,
            cells = evaluateModelResult.cells,
            excel = bytes
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

    private fun findZacksFundamentalA(ticker: String): List<ZacksFundamentalA> {
        return zacksFundamentalAService.getZacksFundamentalAs(ticker)
    }

}
