package com.starburst.starburst.computers

import com.starburst.starburst.models.Cell
import com.starburst.starburst.models.Model

/**
 * Takes in a list of drivers + items
 * and creates a flat representation which cells at each period
 * thus forming a 2-dimensional sheet of cells similar to a spreadsheet
 *
 * [ModelToCellTranslator] does not resolve the dependencies in between cells or populate / validate the expressions
 * the cells will be created with the required references and names only - it's like a skeleton without any of the
 * math
 */
class ModelToCellTranslator {

    fun generateCells(model: Model): List<Cell> {

        val periods = model.periods
        // TODO set the cell Address as well here

        return (1..periods).flatMap { period ->
            model.items.flatMap { item ->
                if (item.expression == null) {
                    // if there are no explicit expressions then
                    // the expression of a Item is just the sum of the drivers
                    val driverCells = (item.drivers ?: emptyList()).map { driver ->
                        Cell(
                            period = period,
                            name = "${driver.name}_Period$period",
                            driver = driver
                        )
                    }

                    // create a cell for the item itself
                    val itemCell = Cell(
                        period = period,
                        name =  "${item.name}_Period$period",
                        item = item,
                        // by default the value of the item is the sum of it's drivers
                        expression = driverCells.joinToString("+") { it.name },
                        dependentCellNames = driverCells.map { it.name }
                    )
                    // end
                    driverCells + itemCell
                } else {
                    listOf(
                        Cell(
                            period = period,
                            item = item,
                            name =  "${item.name}_Period$period"
                        )
                    )
                }
            }
        }

    }

}
