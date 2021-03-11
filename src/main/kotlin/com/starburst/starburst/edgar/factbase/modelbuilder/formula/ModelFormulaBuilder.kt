package com.starburst.starburst.edgar.factbase.modelbuilder.formula

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.*
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model

/**
 * This is the brains of the automated valuation model
 */
class ModelFormulaBuilder(val model: Model, val ctx: ModelFormulaBuilderContext) {

    private val incomeStatementGeneratorChain = listOf(
        InterestFormulaGenerator(),
        NonCashExpenseGenerator(),
        OneTimeExpenseGenerator(),
        RevenueDrivenItemFormulaGenerator(),
        RevenueFormulaGenerator(),
        StockBasedCompensationGenerator(),
        TaxExpenseFormulaGenerator(),
    )

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
        return model.cashFlowStatementItems
    }

    private fun balanceSheet(): List<Item> {
        return model.balanceSheetItems
    }

    private fun incomeStatement(): List<Item> {
        return model.incomeStatementItems.map { item ->
            // skip processing for items that already have formulas
            if (ctx.itemDependencyGraph[item.name].isNullOrEmpty()) {
                incomeStatementGeneratorChain.fold(item) { accItem, generator ->
                    generator.generate(accItem, ctx)
                }
            } else {
                item
            }
        }
    }

}