package com.starburst.starburst.edgar.factbase.modelbuilder.formula

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.*
import com.starburst.starburst.models.GeneratorCommentary
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
        CostOfGoodsSoldFormulaGenerator(),
        RevenueDrivenItemFormulaGenerator(),
        RevenueFormulaGenerator(),
        TaxExpenseFormulaGenerator(),
        OneTimeExpenseGenerator(),
        NonCashExpenseGenerator(),
    )

    private val balanceSheetFormulaGeneratorChain = listOf(
        AverageFormulaGenerator(),
    )

    private val cashFlowStatementFormulaGeneratorChain = listOf(
        OneTimeExpenseGenerator(),
        IncreaseDecreaseFormulaGenerator(),
        AverageFormulaGenerator(),
        NonCashExpenseGenerator(),
        StockBasedCompensationGenerator(),
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

    private fun List<FormulaGenerator>.process(items: List<Item>): List<Item> {
        return items.map { originalItem ->
            if (ctx.itemDependencyGraph[originalItem.name].isNullOrEmpty()) {
                fold(originalItem) {
                        prevItem, generator ->
                    if (generator.relevantForItem(prevItem, ctx)) {
                        val result = generator.generate(prevItem, ctx)
                        /*
                        if the next iteration differs from the previous
                        then add any commentaries + append processor class info for
                        debugging
                         */
                        if (result.item != prevItem || result.commentary != null) {
                            result.item.copy(
                                generatorCommentaries = prevItem.generatorCommentaries +
                                        GeneratorCommentary(
                                            commentary = result.commentary,
                                            generatorClass = generator::class.java.simpleName
                                        )
                            )
                        } else {
                            prevItem
                        }
                    } else {
                        prevItem
                    }
                }
            } else {
                originalItem
            }
        }
    }

    private fun cashFlowStatement(): List<Item> {
        return cashFlowStatementFormulaGeneratorChain.process(model.cashFlowStatementItems)
    }

    private fun balanceSheet(): List<Item> {
        return balanceSheetFormulaGeneratorChain.process(model.balanceSheetItems)
    }

    private fun incomeStatement(): List<Item> {
        return incomeStatementFormulaGeneratorChain.process(model.incomeStatementItems)
    }

}