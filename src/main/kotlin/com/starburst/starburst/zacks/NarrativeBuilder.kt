package com.starburst.starburst.zacks

import com.starburst.starburst.DoubleExtensions.fmtPct
import com.starburst.starburst.DoubleExtensions.orZero
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
import com.starburst.starburst.models.CellGenerator
import com.starburst.starburst.spreadsheet.Cell
import com.starburst.starburst.zacks.dataclasses.KeyInputs
import com.starburst.starburst.zacks.dataclasses.Narrative
import com.starburst.starburst.zacks.dataclasses.Projection
import com.starburst.starburst.zacks.dataclasses.TalkingPoint
import com.starburst.starburst.zacks.modelbuilder.ZacksModelBuilder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@Service
class NarrativeBuilder(
    private val modelBuilder: ZacksModelBuilder,
) {

    private val executor = Executors.newCachedThreadPool()

    private fun Model.valueOf(itemName: String) =
        (incomeStatementItems + cashFlowStatementItems + balanceSheetItems + otherItems)
            .find { item -> item.name == itemName }?.historicalValue?.value.orZero()

    private fun Model.item(itemName: String) =
        (incomeStatementItems + cashFlowStatementItems + balanceSheetItems + otherItems)
            .find { item -> item.name == itemName }

    private fun List<Cell>.projectionsFor(itemName: String): List<Projection> {
        val year = LocalDate.now().year
        return this
            .filter { cell ->
                cell.item.name == itemName && cell.period > 0
            }
            .map { cell ->
                val period = cell.period
                Projection(year = year + period, value = cell.value.orZero())
            }
    }

    /**
     * Run the model and export it's Excel workbook representation as bytes
     */
    fun exportExcel(ticker: String): ByteArray {
        val results = modelBuilder.buildModel(ticker)
        return CellGenerator.exportToXls(results.evaluateModelResult.model, results.evaluateModelResult.cells)
    }

    /**
     * Creates a narrative from models
     */
    fun buildNarrative(ticker: String): Narrative {
        val resp = modelBuilder.buildModel(ticker)
        val model = resp.evaluateModelResult.model
        val cells = resp.evaluateModelResult.cells

        val incomeStatementItems = model.incomeStatementItems

        val noGrowthValueTalkingPoint = executor.submit(Callable { noGrowthValueTalkingPt(ticker) })

        val revenueItem = model.item(Revenue)
        val revCAGR = revCAGRComputation(revenueItem)
        val revenueTalkingPoint = TalkingPoint(
            data = revenueItem?.historicalValue?.value,
            commentary = revenueItem?.commentaries?.commentary,
            forwardCommentary = """
            |Zack's research estimates revenue growth to be ${revCAGR.fmtPct()}. 
            |We will use this projection to derive the target stock price
            """.trimMargin(),
            projections = cells.projectionsFor(Revenue)
        )

        val cogsItem = model.item(CostOfGoodsSold)
        val variableCostTalkingPoint = TalkingPoint(
            data = cogsItem?.historicalValue?.value?.orZero(),
            forwardCommentary = cogsItem?.commentaries?.commentary,
        )
        val operatingExpense = incomeStatementItems.find { it.name == OperatingExpense }
        val fixedCostTalkingPoint = TalkingPoint(
            data = operatingExpense?.historicalValue?.value,
            forwardCommentary = operatingExpense?.commentaries?.commentary,
        )

        val otherExpensesTalkingPoint = otherExpensesTalkingPt(incomeStatementItems)
        val netIncomeTalkingPoint = TalkingPoint(
            data = model.valueOf(NetIncome),
            projections = cells.projectionsFor(NetIncome)
        )

        val epsTalkingPoint = epsTalkingPt(incomeStatementItems, model)
        val growthTalkingPoint = TalkingPoint(data = revCAGR)

        val targetPriceTalkingPoint = TalkingPoint(
            data = resp.evaluateModelResult.targetPrice,
        )

        return Narrative(
            model = model,
            revenueTalkingPoint = revenueTalkingPoint,
            variableCostTalkingPoint = variableCostTalkingPoint,
            fixedCostTalkingPoint = fixedCostTalkingPoint,
            otherExpensesTalkingPoint = otherExpensesTalkingPoint,
            netIncomeTalkingPoint = netIncomeTalkingPoint,
            epsTalkingPoint = epsTalkingPoint,
            noGrowthValueTalkingPoint = noGrowthValueTalkingPoint.get(),
            growthTalkingPoint = growthTalkingPoint,
            targetPriceTalkingPoint = targetPriceTalkingPoint,
        )
    }

    /**
     * Rerun the model with 0 revenue growth by submitting [KeyInputs] that
     * holds revenue constant
     */
    private fun noGrowthValueTalkingPt(ticker: String): TalkingPoint {
        val noGrowthResp = modelBuilder.buildModel(
            ticker = ticker,
            revenueKeyInputs = KeyInputs(
                _id = "",
                formula = previous(Revenue)
            )
        )
        return TalkingPoint(
            data = noGrowthResp.evaluateModelResult.targetPrice,
        )
    }

    private fun epsTalkingPt(
        incomeStatementItems: List<Item>,
        model: Model
    ): TalkingPoint {
        val netIncome = incomeStatementItems.find { it.name == NetIncome }?.historicalValue?.value ?: 0.0
        val sharesOutstanding =
            model.balanceSheetItems.find { it.name == SharesOutstanding }?.historicalValue?.value ?: 0.0
        return TalkingPoint(
            data = netIncome / sharesOutstanding,
        )
    }

    private fun otherExpensesTalkingPt(incomeStatementItems: List<Item>): TalkingPoint {
        val nonOpExp = incomeStatementItems.find { it.name == NonOperatingExpense }?.historicalValue?.value ?: 0.0
        val taxExp = incomeStatementItems.find { it.name == TaxExpense }?.historicalValue?.value ?: 0.0
        val intExp = incomeStatementItems.find { it.name == InterestExpense }?.historicalValue?.value ?: 0.0
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