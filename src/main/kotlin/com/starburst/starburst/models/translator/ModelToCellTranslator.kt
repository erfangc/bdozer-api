package com.starburst.starburst.models.translator

import com.starburst.starburst.spreadsheet.Cell
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

        return (0..periods).flatMap { period ->
            val incomeStatementCells = model.incomeStatementItems.flatMap { item ->
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
                        name = "${item.name}_Period$period",
                        item = item,
                        // by default the value of the item is the sum of it's drivers
                        formula = driverCells.joinToString("+") { it.name },
                        dependentCellNames = driverCells.map { it.name }
                    )
                    // end
                    driverCells + itemCell
                } else {
                    listOf(
                        Cell(
                            period = period,
                            item = item,
                            name = "${item.name}_Period$period"
                        )
                    )
                }
            }

            /*
            create the balance sheet cells
             */
            val balanceSheetCells = model.balanceSheetItems.map { item ->
                Cell(
                    name = "${item.name}_Period$period",
                    formula = item.expression,
                    item = item,
                    period = period
                )
            }

            /*
            create the other cells
             */
            val otherCells = model.otherItems.map { item ->
                Cell(
                    name = "${item.name}_Period$period",
                    formula = item.expression,
                    item = item,
                    period = period
                )
            }

            incomeStatementCells + balanceSheetCells + otherCells
        }
    }

}
