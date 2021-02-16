package com.starburst.starburst.computers

import com.starburst.starburst.computers.drivers.SaaSRevenueExpressionResolver
import com.starburst.starburst.models.Cell
import com.starburst.starburst.models.DriverType
import com.starburst.starburst.models.Model

/**
 * Takes cells and based on their driver resolves
 * an expression for the cell's value as a function of other cells
 * as well as reference data. Populates the "dependency" property as well as "expression" string
 *
 * The ability to name dependency explicitly within each cell allows a dependency graph to be created
 */
class CellExperssionResolver {
    fun resolveCellExpressions(
        model: Model,
        cells: List<Cell>
    ): List<Cell> {
        return cells.map { cell ->

            val driver = cell.driver
            val period = cell.period

            when (driver.type) {
                DriverType.SaaSRevenue -> SaaSRevenueExpressionResolver().resolveExpression(model, cell)
                DriverType.VariableCost -> {
                    val percentOfRevenue = driver.variableCost?.percentOfRevenue ?: 0.0
                    cell.copy(
                        expression = "Revenue_Period$period * $percentOfRevenue",
                        dependencies = listOf("Revenue_Period$period")
                    )
                }
                DriverType.FixedCost -> {
                    val fixedCost = driver.fixedCost ?: error("")
                    cell.copy(
                        expression = "${fixedCost.cost}",
                        dependencies = emptyList()
                    )
                }
            }

        }
    }
}
