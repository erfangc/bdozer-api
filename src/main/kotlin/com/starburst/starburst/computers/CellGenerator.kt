package com.starburst.starburst.computers

import com.starburst.starburst.models.Cell
import com.starburst.starburst.models.Model

/**
 * Takes in a list of drivers and a projection period
 * then creates a list of cells representing each driver's value
 * at each period - thus forming a 2-dimensional sheet of cells similar to a spreadsheet
 *
 * [CellGenerator] does not resolve the dependencies in between cells or populate / validate the expressions
 * the cells will be created with the required references and names only - it's like a skeleton without any of the
 * math
 */
class CellGenerator {

    fun generateCells(model: Model): List<Cell> {
        val periods = model.periods ?: 5
        return (1..periods).flatMap { period ->
            model.drivers.map { driver ->
                Cell(
                    period = period,
                    name = "${driver.name}_Period$period",
                    driver = driver
                )
            }
        }

    }

}
