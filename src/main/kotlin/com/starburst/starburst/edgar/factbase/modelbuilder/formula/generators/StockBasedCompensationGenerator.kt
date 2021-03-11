package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.ShareBasedCompensation
import com.starburst.starburst.models.Item

class StockBasedCompensationGenerator : FormulaGenerator {
    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Item {
        return if (item.name == ShareBasedCompensation) {
            item.copy(stockBasedCompensation = true)
        } else {
            item
        }
    }
}