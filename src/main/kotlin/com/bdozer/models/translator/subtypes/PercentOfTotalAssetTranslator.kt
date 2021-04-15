package com.bdozer.models.translator.subtypes

import com.bdozer.models.Utility.TotalAsset
import com.bdozer.models.Utility.previous
import com.bdozer.models.translator.FormulaTranslationContext
import com.bdozer.spreadsheet.Cell

class PercentOfTotalAssetTranslator(private val ctx: FormulaTranslationContext) : FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val percentOfTotalAsset =
            cell.item.percentOfTotalAsset?.percentOfTotalAsset ?: error("percentOfTotalAsset must be populated")
        return cell.copy(formula = "${previous(TotalAsset)}*$percentOfTotalAsset")
    }
}
