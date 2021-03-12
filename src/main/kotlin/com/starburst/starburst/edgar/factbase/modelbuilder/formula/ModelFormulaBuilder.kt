package com.starburst.starburst.edgar.factbase.modelbuilder.formula

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.withCommentary
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.Result
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.generalized.AverageFormulaGenerator
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.generalized.OneTimeExpenseGenerator
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.generalized.PercentOfRevenueFormulaGenerator
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.generalized.RevenueDrivenFormulaGenerator
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.itemized.ItemizedFormulaGenerator
import com.starburst.starburst.models.dataclasses.GeneratorCommentary
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.Model
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * This is the brains of the automated valuation model
 */
@Service
class ModelFormulaBuilder(
    private val itemizedFormulaGenerator: ItemizedFormulaGenerator,
    /*
    Generalized
     */
    private val averageFormulaGenerator: AverageFormulaGenerator,
    private val oneTimeExpenseGenerator: OneTimeExpenseGenerator,
    private val percentOfRevenueFormulaGenerator: PercentOfRevenueFormulaGenerator,
    private val revenueDrivenFormulaGenerator: RevenueDrivenFormulaGenerator,
) {

    private val log = LoggerFactory.getLogger(ModelFormulaBuilder::class.java)

    private fun <T> withCommentary(result: Result, clazz: Class<T>): Item {
        return result
            .item
            .copy(
                generatorCommentaries = listOf(
                    GeneratorCommentary(commentary = result.commentary, generatorClass = clazz.simpleName)
                )
            )
    }

    private fun processItems(items: List<Item>, ctx: ModelFormulaBuilderContext): List<Item> {
        return items.map { item -> processItem(item, ctx) }
    }

    private fun processItem(item: Item, ctx: ModelFormulaBuilderContext): Item {
        if (ctx.itemDependencyGraph[item.name].isNullOrEmpty()) {
            val firstAttempt = itemizedFormulaGenerator.generate(item = item, ctx = ctx)
            if (firstAttempt.item != item) {
                return withCommentary(firstAttempt, itemizedFormulaGenerator.javaClass)
            } else {
                log.info("Unable to find an itemized formula process for ${item.name}, trying out generalized")
                val chain = listOf(
                    percentOfRevenueFormulaGenerator,
                    revenueDrivenFormulaGenerator,
                    oneTimeExpenseGenerator,
                    // this is the catch all so it comes last
                    averageFormulaGenerator
                )
                val generator = chain.find { generator -> generator.relevantForItem(item, ctx) }
                return generator?.let { generator ->
                    log.info("Found generalized generator ${generator.javaClass.simpleName} to accept ${item.name}")
                    val result = generator.generate(item, ctx)
                    generator.withCommentary(result)
                } ?: item
            }
        } else {
            return item
        }
    }

    /**
     * Takes as input model that is already linked via the calculationArcs
     * and with historical values for the items populated
     *
     * This is the master method for which we begin to populate formulas and move them beyond
     * simply 0.0 or repeating historical
     */
    fun buildModel(ctx: ModelFormulaBuilderContext): Model {
        val model = ctx.model
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
            incomeStatementItems = processItems(incomeStatementItems, ctx),
            balanceSheetItems = processItems(balanceSheetItems, ctx),
            cashFlowStatementItems = processItems(cashFlowItems, ctx)
        )
    }

}
