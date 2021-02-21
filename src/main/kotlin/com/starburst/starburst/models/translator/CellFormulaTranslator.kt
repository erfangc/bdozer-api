package com.starburst.starburst.models.translator

import com.starburst.starburst.spreadsheet.Cell
import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.models.DriverType
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.translator.subtypes.*

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

            val driver = cell.driver
            val item = cell.item
            val period = cell.period


            /*
            First, handle the initial case where the cell represents an Item at period = 0
            in this case, we actually short circuit the formula specified by the item or driver
            and replace the cell's formula to be just it's historical value of the underlying item or
            driver (defaults to zero)
             */
            if (period == 0) {
                val historicalValue = item?.historicalValue ?: driver?.historicalValue ?: 0.0
                cell.copy(formula = "$historicalValue")
            }
            /*
            Next, we handle cases where an Item has a formula defined, in this case
            driver formula is ignored
             */
            else if (item != null) {
                if (item.expression != null) {
                    val updatedCell = cell.copy(formula = item.expression)
                    GenericExpressionTranslator(ctx)
                        .translateFormula(updatedCell)
                } else {
                    cell
                }
            }
            /*
            Finally, the only other way a cell can have a formula is if it belongs to a driver
             */
            else {
                when (driver?.type) {
                    DriverType.SaaSRevenue -> SaaSRevenueExpressionTranslator(ctx)
                        .resolveExpression(cell)

                    DriverType.VariableCost -> VariableCostExpressionTranslator(ctx)
                        .translateFormula(cell)

                    DriverType.FixedCost -> FixedCostExpressionTranslator(ctx)
                        .translateFormula(cell)

                    DriverType.Custom -> CustomExpressionTranslator(ctx)
                        .translateFormula(cell)

                    else -> error("unable to determine driver type")
                }
            }

        }
    }
}
