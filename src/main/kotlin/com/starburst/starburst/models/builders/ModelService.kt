package com.starburst.starburst.models.builders

import com.starburst.starburst.computers.CellEvaluator
import com.starburst.starburst.computers.CellExpressionResolver
import com.starburst.starburst.computers.ModelToCellTranslator
import com.starburst.starburst.models.Cell
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import com.starburst.starburst.computers.ReservedItemNames.CostOfGoodsSold
import com.starburst.starburst.computers.ReservedItemNames.GrossProfit
import com.starburst.starburst.computers.ReservedItemNames.InterestExpense
import com.starburst.starburst.computers.ReservedItemNames.NetIncome
import com.starburst.starburst.computers.ReservedItemNames.NonOperatingExpense
import com.starburst.starburst.computers.ReservedItemNames.OperatingExpense
import com.starburst.starburst.computers.ReservedItemNames.OperatingIncome
import com.starburst.starburst.computers.ReservedItemNames.Revenue
import com.starburst.starburst.computers.ReservedItemNames.TaxExpense
import org.springframework.stereotype.Service

@Service
class ModelService {

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
                item.name == NetIncome -> {
                    val historicalValue = (newItems.find { it.name == OperatingIncome }?.historicalValue ?: 0.0) -
                    (newItems.find { it.name == NonOperatingExpense }?.historicalValue ?: 0.0) -
                    (newItems.find { it.name == InterestExpense }?.historicalValue ?: 0.0) -
                    (newItems.find { it.name == TaxExpense }?.historicalValue ?: 0.0)
                    newItems.add(
                        item.copy(
                            expression = "$OperatingIncome-$NonOperatingExpense-$InterestExpense-$TaxExpense",
                            historicalValue = historicalValue
                        )
                    )
                    buffer.clear()
                }
                item.name == GrossProfit -> {
                    val historicalValue = (newItems.find { it.name == Revenue }?.historicalValue ?: 0.0) -
                            (newItems.find { it.name == CostOfGoodsSold }?.historicalValue ?: 0.0)
                    newItems.add(item.copy(expression = "$Revenue-$CostOfGoodsSold", historicalValue = historicalValue))
                }
                item.name == OperatingIncome -> {
                    val historicalValue = (newItems.find { it.name == GrossProfit }?.historicalValue ?: 0.0) -
                            (newItems.find { it.name == OperatingExpense }?.historicalValue ?: 0.0)
                    newItems.add(item.copy(expression = "$GrossProfit-$OperatingExpense", historicalValue = historicalValue))
                }
                atBreakPoint(item) -> {
                    // clear the buffer if we are at a break point
                    // set the break point's expression to be the sum of the other items
                    val expression = buffer.joinToString("+") { it.name }
                    newItems.add(item.copy(expression = expression, historicalValue = buffer.sumByDouble { it.historicalValue }))
                    buffer.clear()
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
        return setOf(InterestExpense, TaxExpense).contains(item.name)
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
        val generateCells = ModelToCellTranslator().generateCells(model)
        val cells = CellExpressionResolver().resolveCellExpressions(model, generateCells)
        return CellEvaluator().evaluate(model, cells)
    }

}
