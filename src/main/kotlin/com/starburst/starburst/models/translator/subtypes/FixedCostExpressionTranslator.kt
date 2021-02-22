package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.spreadsheet.Cell

class FixedCostExpressionTranslator(private val ctx: ResolverContext): ExpressionTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val item = cell.item
        val fixedCost = item?.fixedCost ?: error("a fixed cost must be specified")
        return cell.copy(
            formula = "${fixedCost.cost}",
            dependentCellNames = emptyList()
        )
    }
}
