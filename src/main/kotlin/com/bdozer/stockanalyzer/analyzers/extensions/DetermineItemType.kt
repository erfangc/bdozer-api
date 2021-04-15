package com.bdozer.stockanalyzer.analyzers.extensions

import com.bdozer.edgar.factbase.modelbuilder.formula.USGaapConstants.IncomeLossFromEquityMethodInvestmentsAndOtherThanTemporaryImpairment
import com.bdozer.edgar.factbase.modelbuilder.formula.USGaapConstants.IncomeTaxExpenseBenefit
import com.bdozer.edgar.factbase.modelbuilder.formula.USGaapConstants.RestructuringAndOtherExpenseIncomeMainline
import com.bdozer.edgar.factbase.modelbuilder.formula.USGaapConstants.SpecialItems
import com.bdozer.models.dataclasses.Item
import com.bdozer.stockanalyzer.analyzers.AbstractStockAnalyzer

object DetermineItemType {

    fun AbstractStockAnalyzer.isCostOperatingCost(item: Item): Boolean {
        return conceptDependencies[operatingCostConceptName]
            ?.map { it.conceptName }
            ?.contains(item.name) == true
    }

    fun AbstractStockAnalyzer.isOneTime(item: Item): Boolean {
        return setOf(
            RestructuringAndOtherExpenseIncomeMainline,
            IncomeLossFromEquityMethodInvestmentsAndOtherThanTemporaryImpairment,
            SpecialItems,
        ).contains(item.name)
    }

    fun AbstractStockAnalyzer.isTaxItem(item: Item): Boolean {
        return item.name == IncomeTaxExpenseBenefit
    }

    fun AbstractStockAnalyzer.isEpsItem(item: Item): Boolean {
        return item.name == epsConceptName
    }


}