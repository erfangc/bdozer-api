package com.bdozer.models.translator.subtypes

import com.bdozer.models.translator.FormulaTranslationContext
import com.bdozer.spreadsheet.Cell

class SumTranslator(private val ctx: FormulaTranslationContext) : FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val period = cell.period
        val item = cell.item
        val components = item.sum?.components ?: error("sum not defined")
        val formula = components.map { "${it.itemName}_Period$period*${it.weight}" }.joinToString("+")
        val dependentCellNames = components.map { "${it.itemName}_Period$period" }
        return cell.copy(
            formula = formula,
            dependentCellNames = dependentCellNames
        )
    }
}