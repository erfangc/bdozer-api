package com.bdozer.api.web.models.translator.subtypes

import com.bdozer.api.web.models.translator.FormulaTranslationContext
import com.bdozer.api.web.spreadsheet.Cell

class PercentOfAnotherItemTranslator(
    private val ctx: FormulaTranslationContext
) : FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val period = cell.period
        val item = cell.item
        val dependentItemName = item
            .percentOfAnotherItem
            ?.itemName ?: error("${item::percentOfAnotherItem.name} must be provided")

        val percent = item.percentOfAnotherItem.percent
        val dependentItem = ctx.model.allItems().find { it.name == dependentItemName }
        if (dependentItem == null) {
            error("$dependentItemName required by ${item.name} cannot be found")
        } else {
            val dependentCellName = "${dependentItemName}_Period$period"
            return cell.copy(
                formula = "$dependentCellName*$percent",
                dependentCellNames = listOf(dependentCellName),
            )
        }
    }
}
