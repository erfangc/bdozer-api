package com.starburst.starburst.models.translator

import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import com.starburst.starburst.spreadsheet.Address
import com.starburst.starburst.spreadsheet.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream

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

    companion object {

        fun exportToXls(model: Model, evaluatedCells: List<Cell>): ByteArrayInputStream {

            fun Sheet.writeHeader(items: List<Item>) {
                val row1 = this.createRow(model.excelRowOffset - 1)
                for (column in 0..model.periods) {
                    row1
                        .createCell(column + model.excelColumnOffset)
                        .setCellValue("Year $column")
                }
                items.forEachIndexed { row, item ->
                    this
                        .createRow(row + model.excelRowOffset)
                        .createCell(model.excelColumnOffset - 1)
                        .setCellValue(item.description ?: item.name)
                }
            }

            val wb: Workbook = XSSFWorkbook()
            val sheet1 = wb.createSheet()
            sheet1.writeHeader(model.incomeStatementItems)

            val sheet2 = wb.createSheet()
            sheet2.writeHeader(model.balanceSheetItems)

            val sheet3 = wb.createSheet()
            sheet3.writeHeader(model.cashFlowStatementItems)

            val sheet4 = wb.createSheet()
            sheet4.writeHeader(model.otherItems)

            evaluatedCells.forEach { cell ->
                val address = cell.address ?: error("...")
                val sheet = wb.getSheetAt(address.sheet)
                val targetRow = address.row - 1
                val row = if (sheet.getRow(targetRow) == null) {
                    sheet.createRow(targetRow)
                } else {
                    sheet.getRow(targetRow)
                }
                val xlsCell = row.createCell(address.column, CellType.FORMULA)
                xlsCell.cellFormula = cell.excelFormula
            }

            val outputStream = ByteArrayOutputStream()
            wb.write(outputStream)
            outputStream.close()
            wb.close()

            return outputStream.toByteArray().inputStream()
        }
    }

    fun generateCells(model: Model): List<Cell> {

        val periods = model.periods

        return (0..periods).flatMap { period ->

            val column = period + model.excelColumnOffset
            val columnLetter = columnOf(column)
            fun idxToRow(idx: Int) = idx + model.excelRowOffset + 1

            /*
            create the income statement sheet cells
             */
            val incomeStatementCells = model.incomeStatementItems.mapIndexed { idx, item ->
                Cell(
                    period = period,
                    item = item,
                    name = "${item.name}_Period$period",
                    address = Address(
                        sheet = 0,
                        row = idxToRow(idx),
                        column = column,
                        columnLetter = columnLetter,
                    )
                )
            }

            /*
            create the balance sheet cells
             */
            val balanceSheetCells = model.balanceSheetItems.mapIndexed { idx, item ->
                Cell(
                    name = "${item.name}_Period$period",
                    formula = item.expression,
                    item = item,
                    period = period,
                    address = Address(
                        sheet = 1,
                        row = idxToRow(idx),
                        column = column,
                        columnLetter = columnLetter,
                    )
                )
            }

            /*
            create the balance sheet cells
             */
            val cashFlowStatementItems = model.cashFlowStatementItems.mapIndexed { idx, item ->
                Cell(
                    name = "${item.name}_Period$period",
                    formula = item.expression,
                    item = item,
                    period = period,
                    address = Address(
                        sheet = 2,
                        row = idxToRow(idx),
                        column = column,
                        columnLetter = columnLetter,
                    )
                )
            }

            /*
            create the other cells
             */
            val otherCells = model.otherItems.mapIndexed { idx, item ->
                Cell(
                    name = "${item.name}_Period$period",
                    formula = item.expression,
                    item = item,
                    period = period,
                    address = Address(
                        sheet = 3,
                        row = idxToRow(idx),
                        column = column,
                        columnLetter = columnLetter,
                    )
                )
            }

            incomeStatementCells + balanceSheetCells + cashFlowStatementItems + otherCells
        }
    }

    private fun columnOf(period: Int): String {
        return when (period) {
            0 -> "A"
            1 -> "B"
            2 -> "C"
            3 -> "D"
            4 -> "E"
            5 -> "F"
            6 -> "G"
            7 -> "H"
            else -> "I"
        }
    }

}
