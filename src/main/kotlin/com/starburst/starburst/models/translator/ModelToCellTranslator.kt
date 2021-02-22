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
            /*
            create the income statement sheet cells
             */
            val incomeStatementCells = model.incomeStatementItems.map { item ->
                Cell(
                    period = period,
                    item = item,
                    name = "${item.name}_Period$period"
                )
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
