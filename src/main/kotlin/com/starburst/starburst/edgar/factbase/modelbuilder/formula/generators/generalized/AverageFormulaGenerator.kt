package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.generalized

import com.starburst.starburst.extensions.DoubleExtensions.fmtRound
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ItemValueExtractorsExtension.itemTimeSeries
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.FormulaGenerator
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.Result
import com.starburst.starburst.models.dataclasses.Item
import org.springframework.stereotype.Service

/**
 * This is the catch all [FormulaGenerator]
 */
@Service
class AverageFormulaGenerator : FormulaGenerator {
    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
        val timeSeries = ctx.itemTimeSeries(itemName = item.name)
        val average = timeSeries.map { it.value ?: 0.0 }.average()
        return if (average.isNaN() || average.isInfinite()) {
            Result(item = item)
        } else {
            Result(
                item = item.copy(
                    formula = "$average"
                ),
                commentary = "We assume this item will proceed forward at it's historical average level of ${average.fmtRound()}"
            )
        }
    }

    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        return true
    }
}