package com.bdozer.api.web.models.translator.subtypes

import bdozer.api.common.spreadsheet.Cell

interface FormulaTranslator {
    fun translateFormula(cell: Cell): Cell
}
