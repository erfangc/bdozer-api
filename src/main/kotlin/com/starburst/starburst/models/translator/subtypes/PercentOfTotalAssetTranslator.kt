package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.computers.ReservedItemNames.TotalAsset
import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.models.Util.previous
import com.starburst.starburst.spreadsheet.Cell

class PercentOfTotalAssetTranslator(private val ctx: ResolverContext) : FormulaTranslator {
    override fun translateFormula(cell: Cell): Cell {
        val percentOfTotalAsset = cell.item.percentOfTotalAsset?.percentOfTotalAsset ?: error("percentOfTotalAsset must be populated")
        return cell.copy(formula = "${previous(TotalAsset)}*$percentOfTotalAsset")
    }
}
