package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.spreadsheet.Cell

interface ExpressionTranslator {
    fun translateFormula(cell: Cell): Cell
}
