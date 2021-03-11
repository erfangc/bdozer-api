package com.starburst.starburst.edgar.factbase.modelbuilder.formula

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.*
import com.starburst.starburst.models.GeneratorCommentary
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.LoggerFactory
import java.io.FileOutputStream

/**
 * This is the brains of the automated valuation model
 */
class ModelFormulaBuilder(val model: Model, val ctx: ModelFormulaBuilderContext) {

    private val log = LoggerFactory.getLogger(ModelFormulaBuilder::class.java)

    private val incomeStatementFormulaGeneratorChain = listOf(
        RevenueFormulaGenerator(),
        CostOfGoodsSoldFormulaGenerator(),
        OperatingExpensesDriver(),
        OneTimeExpenseGenerator(),
        InterestFormulaGenerator(),
        TaxExpenseFormulaGenerator(),
    )

    private val balanceSheetFormulaGeneratorChain = listOf(
        // catch all
        AverageFormulaGenerator(),
    )

    private val cashFlowStatementFormulaGeneratorChain = listOf(
        OneTimeExpenseGenerator(),
        IncreaseDecreaseFormulaGenerator(),
        DepreciationAmortizationFormulaGenerator(),
        StockBasedCompensationGenerator(),
        // catch all
        AverageFormulaGenerator(),
    )

    /**
     * Takes as input model that is already linked via the calculationArcs
     * and with historical values for the items populated
     *
     * This is the master method for which we begin to populate formulas and move them beyond
     * simply 0.0 or repeating historical
     */
    fun buildModelFormula(): Model {
        /*
        de-duplicate item by name,
        the logic as follows: travel all the items arrays
        if a repeat is found, discard it
         */
        val seenSoFar = hashSetOf<Item>()
        fun removeDuplicates(items: List<Item>): List<Item> {
            return items
                .mapNotNull { item ->
                    if (seenSoFar.contains(item)) {
                        log.info("Removed duplicate item ${item.name}")
                        null
                    } else {
                        seenSoFar.add(item)
                        item
                    }
                }
        }

        val incomeStatementItems = removeDuplicates(model.incomeStatementItems)
        val balanceSheetItems = removeDuplicates(model.balanceSheetItems)
        val cashFlowItems = removeDuplicates(model.cashFlowStatementItems)

        return model.copy(
            incomeStatementItems = incomeStatementFormulaGeneratorChain.process(incomeStatementItems),
            balanceSheetItems = balanceSheetFormulaGeneratorChain.process(balanceSheetItems),
            cashFlowStatementItems = cashFlowStatementFormulaGeneratorChain.process(cashFlowItems)
        )
    }

    private fun List<FormulaGenerator>.process(items: List<Item>): List<Item> {
        return items.map { item ->
            if (ctx.itemDependencyGraph[item.name].isNullOrEmpty()) {
                /*
                process a single item by going down the chain and finding the first formula generator
                that would accept this item
                */
                val generator = find { generator -> generator.relevantForItem(item, ctx) }
                generator?.generate(item, ctx)?.let { result ->
                    log.info("Found generator ${className(generator)} for ${item.name}")
                    result.item.copy(
                        generatorCommentaries = listOf(generatorCommentary(result, generator))
                    )
                } ?: item
            } else {
                item
            }
        }
    }

    private fun generatorCommentary(
        result: Result,
        generator: FormulaGenerator
    ) = GeneratorCommentary(
        commentary = result.commentary,
        generatorClass = className(generator)
    )

    private fun className(generator: FormulaGenerator) = generator::class.java.simpleName

}
