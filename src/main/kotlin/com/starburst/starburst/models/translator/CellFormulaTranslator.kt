package com.starburst.starburst.models.translator

import com.starburst.starburst.models.ResolverContext
import com.starburst.starburst.models.dataclasses.ItemType
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.models.translator.subtypes.*
import com.starburst.starburst.spreadsheet.Cell

/**
 * Takes cells and based on their driver resolves
 * an expression for the cell's value as a function of other cells
 * as well as reference data. Populates the "dependency" property as well as "expression" string
 *
 * The ability to name dependency explicitly within each cell allows a dependency graph to be created
 */
class CellFormulaTranslator {
    fun populateCellsWithFormulas(
        model: Model,
        cells: List<Cell>
    ): List<Cell> {

        val ctx = ResolverContext(model = model, cells = cells)

        return cells.map { cell ->
            val item = cell.item
            val period = cell.period

            /*
            First, handle the initial case where the cell represents an Item at period = 0
            in this case, we actually short circuit the formula specified by the item or driver
            and replace the cell's formula to be just it's historical value of the underlying item or
            driver (defaults to zero)
             */
            val updatedCell = if (period == 0) {
                val historicalValue = item.historicalValue
                cell.copy(
                    formula = "$historicalValue"
                )
            }
            /*
            Next, we find the correct formula for the cell depending on it's item and period
             */
            else {
                when (item.type) {
                    ItemType.SubscriptionRevenue -> SubscriptionRevenueTranslator(ctx)
                        .resolveExpression(cell)

                    ItemType.PercentOfRevenue -> PercentOfRevenueTranslator(ctx)
                        .translateFormula(cell)

                    ItemType.PercentOfTotalAsset -> PercentOfTotalAssetTranslator(ctx)
                        .translateFormula(cell)

                    ItemType.FixedCost -> FixedCostTranslator(ctx)
                        .translateFormula(cell)

                    ItemType.UnitSalesRevenue -> UnitSalesRevenueTranslator(ctx)
                        .translateFormula(cell)

                    ItemType.Custom -> CustomTranslator(ctx)
                        .translateFormula(cell)
                }
            }
            updatedCell
        }
    }
}
