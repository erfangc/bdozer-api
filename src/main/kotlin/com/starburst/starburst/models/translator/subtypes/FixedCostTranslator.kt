package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.models.ResolverContext
import com.starburst.starburst.spreadsheet.Cell

class FixedCostTranslator(private val ctx: ResolverContext): FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val item = cell.item
        val fixedCost = item.fixedCost ?: error("a fixed cost must be specified")
        return cell.copy(
            formula = "${fixedCost.cost}",
            dependentCellNames = emptyList()
        )
    }
}
