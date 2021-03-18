package com.starburst.starburst.zacks

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.fmtPct
import com.starburst.starburst.models.Utility.CostOfGoodsSold
import com.starburst.starburst.models.Utility.InterestExpense
import com.starburst.starburst.models.Utility.NetIncome
import com.starburst.starburst.models.Utility.NonOperatingExpense
import com.starburst.starburst.models.Utility.OperatingExpense
import com.starburst.starburst.models.Utility.Revenue
import com.starburst.starburst.models.Utility.SharesOutstanding
import com.starburst.starburst.models.Utility.TaxExpense
import com.starburst.starburst.models.Utility.previous
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.zacks.dataclasses.Narrative
import com.starburst.starburst.zacks.dataclasses.TalkingPoint
import com.starburst.starburst.zacks.modelbuilder.ZacksModelBuilder
import com.starburst.starburst.zacks.modelbuilder.keyinputs.KeyInputs
import org.springframework.stereotype.Service

@Service
class NarrativeBuilder(
    private val modelBuilder: ZacksModelBuilder,
) {

    fun exportExcel(ticker: String): ByteArray {
        return modelBuilder.buildModel(ticker).excel
    }

    fun buildNarrative(ticker: String): Narrative {

        val resp = modelBuilder.buildModel(ticker)
        val model = resp.model
        val incomeStatementItems = model.incomeStatementItems

        val revenueItem = incomeStatementItems.find { it.name == Revenue }
        val revCAGR = revCAGRComputation(revenueItem)
        val revenueTalkingPoint = TalkingPoint(
            data = revenueItem?.historicalValue,
            commentary = revenueItem?.commentaries?.commentary,
            forwardCommentary = "Zack's research estimates revenue growth to be ${revCAGR.fmtPct()}. We will use this projection to derive the target stock price",
        )

        val cogsItem = incomeStatementItems.find { it.name == CostOfGoodsSold }
        val variableCostTalkingPoint = TalkingPoint(
            data = cogsItem?.historicalValue,
            forwardCommentary = cogsItem?.commentaries?.commentary,
        )

        val opExp = incomeStatementItems.find { it.name == OperatingExpense }
        val fixedCostTalkingPoint = TalkingPoint(
            data = opExp?.historicalValue,
            forwardCommentary = opExp?.commentaries?.commentary,
        )

        val otherExpensesTalkingPoint = otherExpensesTalkingPt(incomeStatementItems)
        val epsTalkingPoint = epsTalkingPt(incomeStatementItems, model)
        val noGrowthValueTalkingPoint = noGrowthValueTalkingPt(ticker)

        val growthTalkingPoint = TalkingPoint(
            data = revCAGR,
        )

        val targetPriceTalkingPoint = TalkingPoint(
            data = resp.targetPrice,
        )

        return Narrative(
            model = model,
            revenueTalkingPoint = revenueTalkingPoint,
            variableCostTalkingPoint = variableCostTalkingPoint,
            fixedCostTalkingPoint = fixedCostTalkingPoint,
            otherExpensesTalkingPoint = otherExpensesTalkingPoint,
            epsTalkingPoint = epsTalkingPoint,
            noGrowthValueTalkingPoint = noGrowthValueTalkingPoint,
            growthTalkingPoint = growthTalkingPoint,
            targetPriceTalkingPoint = targetPriceTalkingPoint,
        )
    }

    private fun noGrowthValueTalkingPt(ticker: String): TalkingPoint {
        val noGrowthResp = modelBuilder.buildModel(ticker, keyInputs = KeyInputs(_id = "", formula = previous(Revenue)))
        return TalkingPoint(
            data = noGrowthResp.targetPrice,
        )
    }

    private fun epsTalkingPt(
        incomeStatementItems: List<Item>,
        model: Model
    ): TalkingPoint {
        val netIncome = incomeStatementItems.find { it.name == NetIncome }?.historicalValue ?: 0.0
        val sharesOutstanding = model.balanceSheetItems.find { it.name == SharesOutstanding }?.historicalValue ?: 0.0
        return TalkingPoint(
            data = netIncome / sharesOutstanding,
        )
    }

    private fun otherExpensesTalkingPt(incomeStatementItems: List<Item>): TalkingPoint {
        val nonOpExp = incomeStatementItems.find { it.name == NonOperatingExpense }?.historicalValue ?: 0.0
        val taxExp = incomeStatementItems.find { it.name == TaxExpense }?.historicalValue ?: 0.0
        val intExp = incomeStatementItems.find { it.name == InterestExpense }?.historicalValue ?: 0.0
        return TalkingPoint(
            data = nonOpExp + taxExp + intExp,
        )
    }

    private fun revCAGRComputation(revenueItem: Item?): Double {
        val revs = revenueItem
            ?.discrete
            ?.formulas
            ?.entries
            ?.sortedBy { it -> it.key }
        val last = revs?.last()?.value?.toDouble() ?: error("...")
        val first = revs.first().value.toDouble()
        return last / first - 1.0
    }
}