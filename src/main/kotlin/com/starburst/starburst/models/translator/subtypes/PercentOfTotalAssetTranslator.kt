package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.models.Utility.TotalAsset
import com.starburst.starburst.models.Utility.previous
import com.starburst.starburst.models.translator.FormulaTranslationContext
import com.starburst.starburst.spreadsheet.Cell

class PercentOfTotalAssetTranslator(private val ctx: FormulaTranslationContext) : FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val percentOfTotalAsset = cell.item.percentOfTotalAsset?.percentOfTotalAsset ?: error("percentOfTotalAsset must be populated")
        return cell.copy(formula = "${previous(TotalAsset)}*$percentOfTotalAsset")
    }
}
