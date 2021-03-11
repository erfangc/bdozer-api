package com.starburst.starburst.edgar.factbase.modelbuilder.formula

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.*
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model

/**
 * This is the brains of the automated valuation model
 */
class ModelFormulaBuilder(
    val model: Model,
    val ctx: ModelFormulaBuilderContext
) {

    private val incomeStatementFormulaGeneratorChain = listOf(
        InterestFormulaGenerator(),
        NonCashExpenseGenerator(),
        OneTimeExpenseGenerator(),
        RevenueDrivenItemFormulaGenerator(),
        RevenueFormulaGenerator(),
        StockBasedCompensationGenerator(),
        TaxExpenseFormulaGenerator(),
    )

    private val balanceSheetFormulaGeneratorChain = listOf<FormulaGenerator>(
        AverageFormulaGenerator()
    )

    private val cashFlowStatementFormulaGeneratorChain = listOf<FormulaGenerator>(
        AverageFormulaGenerator()
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
        return model.cashFlowStatementItems.map { item ->
            /*
            skip processing for items that already have formulas
             */
            if (ctx.itemDependencyGraph[item.name].isNullOrEmpty()) {
                /*
                 this fold takes an item - and then run it through every
                 generator in-order
                 */
                cashFlowStatementFormulaGeneratorChain.fold(item) { prev, generator ->
                    /*
                    once we encounter a generator that returned a different item than the one passed in
                    then we stop further processing
                     */
                    if (prev == item) {
                        generator.generate(prev, ctx)
                    } else {
                        prev
                    }
                }
            } else {
                /*
                we do not put any items that has an existing formula through the chain
                since their values are pre-determined
                 */
                item
            }
        }
    }

    private fun balanceSheet(): List<Item> {
        return model.balanceSheetItems.map { item ->
            /*
            skip processing for items that already have formulas
             */
            if (ctx.itemDependencyGraph[item.name].isNullOrEmpty()) {
                /*
                 this fold takes an item - and then run it through every
                 generator in-order
                 */
                balanceSheetFormulaGeneratorChain.fold(item) { prev, generator ->
                    /*
                    once we encounter a generator that returned a different item than the one passed in
                    then we stop further processing
                     */
                    if (prev == item) {
                        generator.generate(prev, ctx)
                    } else {
                        prev
                    }
                }
            } else {
                /*
                we do not put any items that has an existing formula through the chain
                since their values are pre-determined
                 */
                item
            }
        }
    }

    private fun incomeStatement(): List<Item> {
        return model.incomeStatementItems.map { item ->
            /*
            skip processing for items that already have formulas
             */
            if (ctx.itemDependencyGraph[item.name].isNullOrEmpty()) {
                /*
                 this fold takes an item - and then run it through every
                 generator in-order
                 */
                incomeStatementFormulaGeneratorChain.fold(item) { prev, generator ->
                    /*
                    once we encounter a generator that returned a different item than the one passed in
                    then we stop further processing
                     */
                    if (prev == item) {
                        generator.generate(prev, ctx)
                    } else {
                        prev
                    }
                }
            } else {
                /*
                we do not put any items that has an existing formula through the chain
                since their values are pre-determined
                 */
                item
            }
        }
    }

}