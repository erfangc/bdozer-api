package com.bdozer.api.web.models.translator.subtypes

import com.bdozer.api.web.spreadsheet.Cell

interface FormulaTranslator {
    fun translateFormula(cell: Cell): Cell
}
