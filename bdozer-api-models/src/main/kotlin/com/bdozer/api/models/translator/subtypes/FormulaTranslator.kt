package com.bdozer.api.models.translator.subtypes

import com.bdozer.api.models.dataclasses.spreadsheet.Cell

interface FormulaTranslator {
    fun translateFormula(cell: Cell): Cell
}
