package com.starburst.starburst.edgar.factbase.modelbuilder

import com.starburst.starburst.edgar.factbase.modelbuilder.ItemExtensions.nonCashChain
import com.starburst.starburst.edgar.factbase.modelbuilder.ItemExtensions.stockBasedCompensationChain
import com.starburst.starburst.edgar.factbase.modelbuilder.ModelFormulaBuilderExtensions.formulateFixedCostItem
import com.starburst.starburst.edgar.factbase.modelbuilder.ModelFormulaBuilderExtensions.formulateOneTimeItem
import com.starburst.starburst.edgar.factbase.modelbuilder.ModelFormulaBuilderExtensions.formulateRevenueDrivenItem
import com.starburst.starburst.edgar.factbase.modelbuilder.ModelFormulaBuilderExtensions.formulateRevenueItem
import com.starburst.starburst.edgar.factbase.modelbuilder.ModelFormulaBuilderExtensions.formulateTotalAssetDrivenItem
import com.starburst.starburst.edgar.factbase.modelbuilder.ModelFormulaBuilderExtensions.isOneTime
import com.starburst.starburst.edgar.factbase.modelbuilder.ModelFormulaBuilderExtensions.isRevenue
import com.starburst.starburst.edgar.factbase.modelbuilder.ModelFormulaBuilderExtensions.isRevenueDriven
import com.starburst.starburst.edgar.factbase.modelbuilder.ModelFormulaBuilderExtensions.isTotalAssetDriven
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model

/**
 * This is the brains of the automated valuation model
 */
class ModelFormulaBuilder(
    val model: Model,
    val ctx: ModelFormulaBuilderContext
) {

    /**
     * Takes as input model that is already linked via the calculationArcs
     * and with historical values for the items populated
     *
     * This is the master method for which we begin to populate formulas and move them beyond
     * simply 0.0 or repeating historical
     */
    fun buildModelFormula(): Model {
        return model.copy(
            incomeStatementItems = incomeStatement(),
            balanceSheetItems = balanceSheet(),
            cashFlowStatementItems = cashFlowStatement()
        )
    }

    private fun cashFlowStatement(): List<Item> {
        TODO("Not yet implemented")
    }

    private fun balanceSheet(): List<Item> {
        TODO("Not yet implemented")
    }

    private fun incomeStatement(): List<Item> {

        return model.incomeStatementItems.map { item ->
            val formulatedItem = when {
                /*
                for any items for which there exists an existing calculationArc specified
                calculation return it without further processing
                */
                ctx.itemDependencyGraph[item.name]
                    ?.isNotEmpty() == true -> {
                    item
                }
                /*
                treat revenue item(s) using revenue like formulas
                 */
                isRevenue(item) -> {
                    formulateRevenueItem(item)
                }
                isOneTime(item) -> {
                    formulateOneTimeItem(item)
                }
                /*
                if this is a cost item that should be revenue driven then
                process it using [formulateRevenueDrivenItem]
                 */
                isRevenueDriven(item) -> {
                    formulateRevenueDrivenItem(item)
                }
                isTotalAssetDriven(item) -> {
                    formulateTotalAssetDrivenItem(item)
                }
                !isRevenueDriven(item) -> {
                    formulateFixedCostItem(item)
                }
                else -> {
                    item
                }
            }

            /*
            Determine if the item is something that requires some additional process
            and flagging

            Here we use private extension classes to chain them linearly rather than
            nesting the processing

            Taking advantage of a great but controversial Kotlin lang feature
            for readability - please do not export these chain functions, as they make no semantic sense
            outside the context of this class
             */
            formulatedItem
                .stockBasedCompensationChain()
                .nonCashChain(this)
        }

    }


}