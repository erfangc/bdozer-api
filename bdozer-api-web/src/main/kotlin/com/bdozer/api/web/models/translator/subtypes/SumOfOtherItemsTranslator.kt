package com.bdozer.api.web.models.translator.subtypes

import com.bdozer.api.web.models.translator.FormulaTranslationContext
import bdozer.api.common.spreadsheet.Cell

class SumOfOtherItemsTranslator(private val ctx: FormulaTranslationContext) : FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val period = cell.period
        val item = cell.item
        val components = item.sumOfOtherItems?.components ?: error("sumOfOtherItems not defined")
        val formula = components.map { "${it.itemName}_Period$period*${it.weight}" }.joinToString("+")
        val dependentCellNames = components.map { "${it.itemName}_Period$period" }
        return cell.copy(
            formula = formula,
            dependentCellNames = dependentCellNames
        )
    }
}