package com.starburst.starburst.dcf

import com.starburst.starburst.models.ReservedItemNames.FreeCashFlow
import com.starburst.starburst.models.ReservedItemNames.SharesOutstanding
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.ModelEvaluationOutput
import com.starburst.starburst.spreadsheet.Cell
import kotlin.math.pow

class DCFCalculator(private val model: Model) {

    fun calcPv(cells: List<Cell>): ModelEvaluationOutput {
        val fcfCells = cells.filter { it.item.name == FreeCashFlow }.sortedBy { it.period }
        val sharesOutstandingCells = cells.filter { it.item.name == SharesOutstanding }.sortedBy { it.period }

        if (fcfCells.isEmpty())
            error("cannot find $FreeCashFlow cells")

        val lastFcfCell = fcfCells.last()
        val lastSharesOutstandingCell = sharesOutstandingCells.last()

        val discountRate = (model.equityRiskPremium * model.beta) + model.riskFreeRate

        val discountFactors = fcfCells.map { cell ->
            1 / (1.0 + discountRate).pow(cell.period)
        }

        val pvOfFcf = fcfCells.zip(discountFactors).sumByDouble {
            (cell, discountFactor) ->
            val fcf = cell.value ?: error("$FreeCashFlow cell ${cell.name} has no value")
            fcf * discountFactor
        }

        val terminalFcf = lastFcfCell.value ?: error("")

        val lastDiscountFactor = discountFactors.last()
        val pvOfTerminalValueUnderExitMultipleMethod = (terminalFcf * model.terminalFcfMultiple) * lastDiscountFactor

        val pvOfTerminalValueUnderPerpetuityMethod = (terminalFcf / (discountRate - model.terminalFcfGrowthRate)) * lastDiscountFactor

        val sharesOutstanding = lastSharesOutstandingCell.value ?: error("$SharesOutstanding cell has no value")
        val targetPriceUnderExitMultipleMethod = (pvOfFcf + pvOfTerminalValueUnderExitMultipleMethod) / sharesOutstanding
        val targetPriceUnderPerpetuityMethod = (pvOfFcf + pvOfTerminalValueUnderPerpetuityMethod) / sharesOutstanding

        return ModelEvaluationOutput(
            targetPriceUnderExitMultipleMethod = targetPriceUnderExitMultipleMethod,
            targetPriceUnderPerpetuityMethod = targetPriceUnderPerpetuityMethod,
            cells = cells,
            pvOfFcf = pvOfFcf,
            pvOfTerminalValueUnderExitMultipleMethod = pvOfTerminalValueUnderExitMultipleMethod,
            pvOfTerminalValueUnderPerpetuityMethod = pvOfTerminalValueUnderPerpetuityMethod
        )

    }
}
