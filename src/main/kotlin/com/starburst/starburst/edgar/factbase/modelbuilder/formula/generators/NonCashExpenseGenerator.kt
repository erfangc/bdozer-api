package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ElementSemanticsExtensions.isDebtFlowItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.models.Item

class NonCashExpenseGenerator : FormulaGenerator {
    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Item {
        val itemName = item.name
        /*
        A non cash expense is one that
        1. is a non-abstract monetary debit item as defined by the company's XSD or us-gaap
        2. starts or ends with the correct keywords
         */
        val isDebitFlowItem = ctx.isDebtFlowItem(item)

        // next test whether it matches the right vocab
        val keywords = listOf("amortization", "depreciation", "impairment")
        val hasDesiredKeyword = keywords.any { keyword ->
            itemName.toLowerCase().startsWith(keyword)
                    || itemName.toLowerCase().endsWith(keyword)
        }

        return if (isDebitFlowItem && hasDesiredKeyword) {
            item.copy(nonCashExpense = true)
        } else {
            item
        }
    }
}