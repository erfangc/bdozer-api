package com.starburst.starburst.models.builders

import com.starburst.starburst.computers.CellEvaluator
import com.starburst.starburst.computers.CellExpressionResolver
import com.starburst.starburst.computers.CellGenerator
import com.starburst.starburst.models.Cell
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.ReservedNames.CostOfGoodsSold
import com.starburst.starburst.models.ReservedNames.GrossProfit
import com.starburst.starburst.models.ReservedNames.InterestExpense
import com.starburst.starburst.models.ReservedNames.NetIncome
import com.starburst.starburst.models.ReservedNames.NonOperatingExpense
import com.starburst.starburst.models.ReservedNames.OperatingExpense
import com.starburst.starburst.models.ReservedNames.OperatingIncome
import com.starburst.starburst.models.ReservedNames.Revenue
import com.starburst.starburst.models.ReservedNames.TaxExpense
import org.springframework.stereotype.Service

@Service
class GenericModelBuilder {

    /**
     * Recreate and ensure correctness of the formula of key items
     * are correct: e.g. Revenue, CostOfGoodsSold
     */
    fun reformulateModel(model: Model): Model {
        // everything until revenue
        val newItems = mutableListOf<Item>()
        val buffer = mutableListOf<Item>()
        model.items.forEach { item ->
            // test if we are at a break point - if not add to buffer
            when {
                atBreakPoint(item) -> {
                    // clear the buffer if we are at a break point
                    // set the break point's expression to be the sum of the other items
                    val expression = buffer.joinToString("+") { it.name }
                    buffer.clear()
                    newItems.add(item.copy(expression = expression))
                }
                shouldSkip(item) -> {
                    // these items don't matter, and they shouldn't
                    // be part of any buffer either
                    newItems.add(item)
                }
                else -> {
                    // these are user added items that belongs on a buffer
                    buffer.add(item)
                    newItems.add(item)
                }
            }
        }
        return model.copy(items = newItems.toList())
    }

    private fun shouldSkip(item: Item): Boolean {
        return setOf(GrossProfit, OperatingIncome, InterestExpense, TaxExpense, NetIncome).contains(item.name)
    }

    private fun atBreakPoint(item: Item): Boolean {
        return setOf(Revenue, CostOfGoodsSold, OperatingExpense, NonOperatingExpense).contains(item.name)
    }

    /**
     * [createModel] creates the skeleton of a basic model
     * this doesn't have to be the only skeleton model available
     */
    fun createModel(): Model {
        return Model(
            items = listOf(
                Item(
                    name = Revenue,
                    expression = "0.0"
                ),
                Item(
                    name = CostOfGoodsSold,
                    expression = "0.0"
                ),
                Item(
                    name = GrossProfit,
                    expression = "$Revenue - $CostOfGoodsSold"
                ),
                Item(
                    name = OperatingExpense,
                    expression = "0.0"
                ),
                Item(
                    name = OperatingIncome,
                    expression = "$GrossProfit - $OperatingExpense"
                ),
                Item(
                    name = NonOperatingExpense,
                    expression = "0.0"
                ),
                Item(
                    name = InterestExpense,
                    expression = "0.0"
                ),
                Item(
                    name = TaxExpense,
                    expression = "0.0"
                ),
                Item(
                    name = NetIncome,
                    expression = "$OperatingIncome - $NonOperatingExpense - $InterestExpense - $TaxExpense"
                )
            ),
            periods = 5
        )
    }

    fun evaluateModel(model: Model): List<Cell> {
        val generateCells = CellGenerator().generateCells(model)
        val cells = CellExpressionResolver().resolveCellExpressions(model, generateCells)
        return CellEvaluator().evaluate(model, cells)
    }

}
