package com.starburst.starburst.models.evaluator.dcf

import com.starburst.starburst.models.Utility.FreeCashFlow
import com.starburst.starburst.models.Utility.FreeCashFlowPerShare
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.models.dataclasses.ModelEvaluationOutput
import com.starburst.starburst.spreadsheet.Cell
import kotlin.math.pow

/**
 * [DCFCalculator] was a temporary experiment
 */
@Deprecated("should use the cells instead")
class DCFCalculator(private val model: Model) {

    fun calcPv(cells: List<Cell>): ModelEvaluationOutput {
        val fcfPerShare = cells.filter { it.item.name == FreeCashFlowPerShare }.sortedBy { it.period }

        if (fcfPerShare.isEmpty())
            error("cannot find $FreeCashFlowPerShare cells")

        val lastFcfCell = fcfPerShare.last()

        val discountRate = (model.equityRiskPremium * model.beta) + model.riskFreeRate

        val discountFactors = fcfPerShare.map { cell ->
            1 / (1.0 + discountRate).pow(cell.period)
        }

        val pvOfFcf = fcfPerShare
            .zip(discountFactors)
            .sumByDouble { (cell, discountFactor) ->
                val fcf = cell.value ?: error("$FreeCashFlow cell ${cell.name} has no value")
                fcf * discountFactor
            }

        val terminalFcf = lastFcfCell.value ?: error("")

        val lastDiscountFactor = discountFactors.last()
        val pvOfTerminalValueUnderExitMultipleMethod = (terminalFcf * model.terminalFcfMultiple) * lastDiscountFactor

        val pvOfTerminalValueUnderPerpetuityMethod =
            (terminalFcf / (discountRate - model.terminalFcfGrowthRate)) * lastDiscountFactor

        val targetPriceUnderExitMultipleMethod = (pvOfFcf + pvOfTerminalValueUnderExitMultipleMethod)
        val targetPriceUnderPerpetuityMethod = (pvOfFcf + pvOfTerminalValueUnderPerpetuityMethod)

        return ModelEvaluationOutput(
            targetPriceUnderExitMultipleMethod = targetPriceUnderExitMultipleMethod,
            targetPriceUnderPerpetuityMethod = targetPriceUnderPerpetuityMethod,
            cells = cells,
            pvOfFcf = pvOfFcf,
            pvOfTerminalValueUnderExitMultipleMethod = pvOfTerminalValueUnderExitMultipleMethod,
            pvOfTerminalValueUnderPerpetuityMethod = pvOfTerminalValueUnderPerpetuityMethod,
            terminalFcf = terminalFcf
        )

    }
}
