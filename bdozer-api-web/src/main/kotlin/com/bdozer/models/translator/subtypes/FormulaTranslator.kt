package com.bdozer.models.translator.subtypes

import com.bdozer.spreadsheet.Cell

interface FormulaTranslator {
    fun translateFormula(cell: Cell): Cell
}
