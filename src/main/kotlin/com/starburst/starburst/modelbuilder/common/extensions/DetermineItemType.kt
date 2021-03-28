package com.starburst.starburst.modelbuilder.common.extensions

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareBasic
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareDiluted
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.IncomeLossFromEquityMethodInvestmentsAndOtherThanTemporaryImpairment
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.IncomeTaxExpenseBenefit
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.RestructuringAndOtherExpenseIncomeMainline
import com.starburst.starburst.modelbuilder.common.AbstractStockAnalyzer
import com.starburst.starburst.models.dataclasses.Item

object DetermineItemType {

    fun AbstractStockAnalyzer.isCostOperatingCost(item: Item): Boolean {
        return conceptDependencies[operatingCostConceptName]
            ?.map { it.conceptName }
            ?.contains(item.name) == true
    }

    fun AbstractStockAnalyzer.isOneTime(item: Item): Boolean {
        return setOf(
            RestructuringAndOtherExpenseIncomeMainline,
            IncomeLossFromEquityMethodInvestmentsAndOtherThanTemporaryImpairment
        ).contains(item.name)
    }

    fun AbstractStockAnalyzer.isTaxItem(item: Item): Boolean {
        return item.name == IncomeTaxExpenseBenefit
    }

    fun AbstractStockAnalyzer.isEpsItem(item: Item): Boolean {
        val candidates = setOf(EarningsPerShareDiluted, EarningsPerShareBasic)
        return candidates.contains(item.name)
    }


}