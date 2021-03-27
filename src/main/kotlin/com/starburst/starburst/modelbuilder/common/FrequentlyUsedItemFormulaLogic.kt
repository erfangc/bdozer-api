package com.starburst.starburst.modelbuilder.common

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareBasic
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareDiluted
import com.starburst.starburst.models.dataclasses.Commentary
import com.starburst.starburst.models.dataclasses.Item

object FrequentlyUsedItemFormulaLogic {

    fun fillOneTimeItem(item: Item) = item.copy(
        formula = "0.0",
        commentaries = Commentary(commentary = "This is a one-time item")
    )

    fun fillEpsItem(item: Item): Item {
        return when (item.name) {
            EarningsPerShareDiluted -> {
                item.copy(
                    formula = "${USGaapConstants.NetIncomeLoss} / ${USGaapConstants.WeightedAverageNumberOfDilutedSharesOutstanding}"
                )
            }
            EarningsPerShareBasic -> {
                item.copy(
                    formula = "${USGaapConstants.NetIncomeLoss} / ${USGaapConstants.WeightedAverageNumberOfSharesOutstandingBasic}"
                )
            }
            else -> {
                item
            }
        }
    }

    fun fillTaxItem(item: Item): Item {
        return item.copy(
            formula = "${USGaapConstants.IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest}*0.12"
        )
    }
}