package com.starburst.starburst.modelbuilder.common.extensions

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants
import com.starburst.starburst.modelbuilder.common.AbstractStockAnalyzer
import com.starburst.starburst.models.dataclasses.Item

object DetermineItemType {

    fun AbstractStockAnalyzer.isCostOperatingCost(item: Item): Boolean {
        return conceptDependencies[operatingCostConceptName]?.map { it.conceptName }?.contains(item.name) == true
    }

    fun AbstractStockAnalyzer.isOneTime(item: Item): Boolean {
        return setOf(
            "RestructuringAndOtherExpenseIncomeMainline",
            "IncomeLossFromEquityMethodInvestmentsAndOtherThanTemporaryImpairment"
        ).contains(item.name)
    }

    fun AbstractStockAnalyzer.isTaxItem(item: Item): Boolean = item.name == USGaapConstants.IncomeTaxExpenseBenefit

    fun AbstractStockAnalyzer.isEpsItem(item: Item) =
        item.name == USGaapConstants.EarningsPerShareBasic || item.name == USGaapConstants.EarningsPerShareDiluted

}