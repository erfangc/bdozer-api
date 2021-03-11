package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions.ItemValueExtractorsExtension.itemTimeSeries
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.models.Item

class AverageFormulaGenerator : FormulaGenerator {
    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Item {
        val timeSeries = ctx.itemTimeSeries(itemName = item.name)
        return item.copy(
            expression = "${timeSeries.map { it.value ?: 0.0 }.average()}"
        )
    }
}