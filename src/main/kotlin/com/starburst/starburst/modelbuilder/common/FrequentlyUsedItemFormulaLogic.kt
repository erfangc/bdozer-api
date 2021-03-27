package com.starburst.starburst.modelbuilder.common

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants
import com.starburst.starburst.models.dataclasses.Commentary
import com.starburst.starburst.models.dataclasses.Item

object FrequentlyUsedItemFormulaLogic {

    fun processOneTimeItem(item: Item) = item.copy(
        formula = "0.0",
        commentaries = Commentary(commentary = "This is a one-time item")
    )

    fun processEpsItem(item: Item): Item {
        return when (item.name) {
            USGaapConstants.EarningsPerShareDiluted -> {
                item.copy(
                    formula = "${USGaapConstants.NetIncomeLoss} / ${USGaapConstants.WeightedAverageNumberOfDilutedSharesOutstanding}"
                )
            }
            USGaapConstants.EarningsPerShareBasic -> {
                item.copy(
                    formula = "${USGaapConstants.NetIncomeLoss} / ${USGaapConstants.WeightedAverageNumberOfSharesOutstandingBasic}"
                )
            }
            else -> {
                item
            }
        }
    }

    fun processTaxItem(item: Item): Item {
        return item.copy(
            formula = "${USGaapConstants.IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest}*0.12"
        )
    }
}