package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.patternbased

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ElementSemanticsExtensions.isDebtFlowItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.FormulaGenerator
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.Result
import com.starburst.starburst.models.dataclasses.Item
import org.springframework.stereotype.Service

@Service
class DepreciationAmortizationFormulaGenerator : FormulaGenerator {

    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
        return Result(
            item.copy(nonCashExpense = true),
            commentary = "Depreciation, amortization and impairments do not require companies to spend cash"
        )
    }

    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        val itemName = item.name
        /*
        A non cash expense is one that
        1. is a non-abstract monetary debit item as defined by the company's XSD or us-gaap
        2. starts or ends with the correct keywords
         */
        val isDebitFlowItem = ctx.isDebtFlowItem(item)

        // next test whether it matches the right vocab
        val hasDesiredKeyword = itemNameHasDesiredKeywords(itemName)
        return isDebitFlowItem && hasDesiredKeyword
    }

    private fun itemNameHasDesiredKeywords(itemName: String): Boolean {
        val keywords = listOf("amortization", "depreciation", "impairment")
        return keywords.any { keyword ->
            itemName.toLowerCase().startsWith(keyword)
                    || itemName.toLowerCase().endsWith(keyword)
        }
    }
}