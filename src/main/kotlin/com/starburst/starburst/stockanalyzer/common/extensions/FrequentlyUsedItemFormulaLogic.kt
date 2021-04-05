package com.starburst.starburst.stockanalyzer.common.extensions

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.CostsAndExpenses
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareBasic
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareDiluted
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.IncomeLossFromContinuingOperationsBeforeIncomeTaxesMinorityInterestAndIncomeLossFromEquityMethodInvestments
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.NetIncomeLoss
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.OperatingCostsAndExpenses
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.WeightedAverageNumberOfDilutedSharesOutstanding
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.WeightedAverageNumberOfSharesOutstandingBasic
import com.starburst.starburst.stockanalyzer.common.AbstractStockAnalyzer
import com.starburst.starburst.models.dataclasses.Commentary
import com.starburst.starburst.models.dataclasses.Item

object FrequentlyUsedItemFormulaLogic {

    fun AbstractStockAnalyzer.fillOneTimeItem(item: Item) = item.copy(
        formula = "0.0",
        commentaries = Commentary(commentary = "This is a one-time item")
    )

    fun AbstractStockAnalyzer.fillEpsItem(item: Item): Item {
        return when (item.name) {
            EarningsPerShareDiluted -> {
                item.copy(
                    formula = "$NetIncomeLoss / $WeightedAverageNumberOfDilutedSharesOutstanding"
                )
            }
            EarningsPerShareBasic -> {
                item.copy(
                    formula = "$NetIncomeLoss / $WeightedAverageNumberOfSharesOutstandingBasic"
                )
            }
            else -> {
                item
            }
        }
    }

    fun AbstractStockAnalyzer.fillTaxItem(item: Item): Item {
        return item.copy(
            formula = "${ebitConceptName}*0.12"
        )
    }

    fun AbstractStockAnalyzer.totalRevenueItemName(): String {
        val candidateConceptNames = setOf(
            USGaapConstants.RevenueFromContractWithCustomerExcludingAssessedTax
        )
        return calculations
            .incomeStatement
            .find {
                candidateConceptNames
                    .contains(it.conceptName)
            }?.conceptName ?: error("Unable to find revenue total item name for $cik")
    }

    fun AbstractStockAnalyzer.ebitItemName(): String {
        val candidateConceptNames = setOf(
            IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest,
            IncomeLossFromContinuingOperationsBeforeIncomeTaxesMinorityInterestAndIncomeLossFromEquityMethodInvestments
        )
        return calculations
            .incomeStatement
            .find { arc -> candidateConceptNames.contains(arc.conceptName) }
            ?.conceptName ?: error("Unable to determine the Earning Before Income Tax conceptName for $cik")
    }

    fun AbstractStockAnalyzer.operatingCostsItemName(): String {
        val candidates = setOf(
            OperatingCostsAndExpenses,
            CostsAndExpenses,
        )

        return calculations
            .incomeStatement
            .find { arc -> candidates.contains(arc.conceptName) }
            ?.conceptName ?: error("Unable to determine the Operating Costs conceptName for $cik")
    }
}