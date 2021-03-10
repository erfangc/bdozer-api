package com.starburst.starburst.edgar.factbase.modelbuilder

import com.starburst.starburst.edgar.factbase.modelbuilder.ModelFormulaBuilderExtensions.isDebtFlowItem
import com.starburst.starburst.models.Item

object ItemExtensions {

    fun Item.nonCashChain(builder: ModelFormulaBuilder): Item {
        val itemName = this.name
        /*
        A non cash expense is one that
        1. is a non-abstract monetary debit item as defined by the company's XSD or us-gaap
        2. starts or ends with the correct keywords
         */
        val isDebitFlowItem = builder.isDebtFlowItem(this)

        // next test whether it matches the right vocab
        val keywords = listOf("amortization", "depreciation", "impairment")
        val hasDesiredKeyword = keywords.any { keyword ->
            itemName.toLowerCase().startsWith(keyword)
                    || itemName.toLowerCase().endsWith(keyword)
        }

        return if (isDebitFlowItem && hasDesiredKeyword) {
            this.copy(nonCashExpense = true)
        } else {
            this
        }
    }

    fun Item.stockBasedCompensationChain(): Item {
        return if (this.name == "ShareBasedCompensation") {
            this.copy(stockBasedCompensation = true)
        } else {
            this
        }
    }

}