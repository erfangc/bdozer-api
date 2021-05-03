package com.bdozer.api.web.models.translator.subtypes

import bdozer.api.common.spreadsheet.Cell

class FixedCostTranslator : FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val item = cell.item
        val fixedCost = item.fixedCost ?: error("a fixed cost must be specified")
        return cell.copy(
            formula = "${fixedCost.cost}",
            dependentCellNames = emptyList()
        )
    }
}
