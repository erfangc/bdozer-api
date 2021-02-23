package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.spreadsheet.Cell

interface FormulaTranslator {
    fun translateFormula(cell: Cell): Cell
}
