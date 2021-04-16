package com.bdozer.stockanalyzer.analyzers.extensions

import com.bdozer.edgar.factbase.USGaapConstants.IncomeLossFromEquityMethodInvestmentsAndOtherThanTemporaryImpairment
import com.bdozer.edgar.factbase.USGaapConstants.IncomeTaxExpenseBenefit
import com.bdozer.edgar.factbase.USGaapConstants.RestructuringAndOtherExpenseIncomeMainline
import com.bdozer.edgar.factbase.USGaapConstants.SpecialItems
import com.bdozer.models.dataclasses.Item
import com.bdozer.stockanalyzer.analyzers.StockAnalyzer

object DetermineItemType {

    fun StockAnalyzer.isOneTime(item: Item): Boolean {
        return setOf(
            RestructuringAndOtherExpenseIncomeMainline,
            IncomeLossFromEquityMethodInvestmentsAndOtherThanTemporaryImpairment,
            SpecialItems,
        ).contains(item.name)
    }

    fun StockAnalyzer.isTaxItem(item: Item): Boolean {
        return item.name == IncomeTaxExpenseBenefit
    }

    fun StockAnalyzer.isEpsItem(item: Item): Boolean {
        return item.name == epsItemName
    }


}