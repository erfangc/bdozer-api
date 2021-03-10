package com.starburst.starburst.edgar.factbase.modelbuilder.formula

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ItemExtensions.nonCashChain
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ItemExtensions.stockBasedCompensationChain
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderExtensions.formulateOneTimeItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderExtensions.formulateRevenueDrivenItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderExtensions.formulateRevenueItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderExtensions.formulateTotalAssetDrivenItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderExtensions.isOneTime
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderExtensions.isRevenue
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderExtensions.isRevenueDriven
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderExtensions.isTotalAssetDriven
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
        // TODO
        return model.cashFlowStatementItems
    }

    private fun balanceSheet(): List<Item> {
        // TODO
        return model.balanceSheetItems
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