package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ItemValueExtractorsExtension.itemTimeSeries
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.CommentaryExtensions.fmtRound
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ElementSemanticsExtensions.isDebtFlowItem
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ItemValueExtractorsExtension.originalItem
import com.starburst.starburst.models.Item

class AverageFormulaGenerator : FormulaGenerator {
    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
        val timeSeries = ctx.itemTimeSeries(itemName = item.name)
        val average = timeSeries.map { it.value ?: 0.0 }.average()
        return if (average.isNaN() || average.isInfinite()) {
            Result(item = item)
        } else{
            Result(
                item = item.copy(
                    expression = "$average"
                ),
                commentary = "We assume this item will proceed forward at it's historical average level of ${average.fmtRound()}"
            )
        }
    }

    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        /*
        this one is only relevant if the item has not been touched
         */
        val originalItem = ctx.originalItem(item.name)
        return item.expression != originalItem?.expression
    }
}