package com.starburst.starburst.models.translator

import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.models.translator.resolvers.*
import com.starburst.starburst.cells.Cell
import com.starburst.starburst.models.DriverType
import com.starburst.starburst.models.Model

/**
 * Takes cells and based on their driver resolves
 * an expression for the cell's value as a function of other cells
 * as well as reference data. Populates the "dependency" property as well as "expression" string
 *
 * The ability to name dependency explicitly within each cell allows a dependency graph to be created
 */
class CellExpressionResolver {
    fun resolveCellExpressions(
        model: Model,
        cells: List<Cell>
    ): List<Cell> {

        val ctx = ResolverContext(model = model, cells = cells)

        return cells.map { cell ->

            val driver = cell.driver
            val item = cell.item

            if (item != null) {
                /*
                Resolve the cell expression for cells that are driven by Item(s)
                 */
                if (cell.item.expression != null) {
                    StringExpressionResolver(ctx).resolveExpression(cell.copy(expression = cell.item.expression))
                } else {
                    cell
                }
            } else {
                /*
                Resolve the cell expression for cells that are driven by Drivers

                Add addition classes of expression resolver here
                 */
                when (driver?.type) {

                    DriverType.SaaSRevenue -> SaaSRevenueExpressionResolver(ctx)
                        .resolveExpression(cell)

                    DriverType.VariableCost -> VariableCostExpressionResolver(ctx)
                        .resolveExpression(cell)

                    DriverType.FixedCost -> FixedCostExpressionResolver(ctx)
                        .resolveExpression(cell)

                    DriverType.Custom -> CustomExpressionResolver(ctx)
                        .resolveExpression(cell)
                    else -> error("unable to determine driver type")
                }
            }

        }
    }
}
