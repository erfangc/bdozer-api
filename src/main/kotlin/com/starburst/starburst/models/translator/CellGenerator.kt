package com.starburst.starburst.models.translator

import com.starburst.starburst.models.ExcelFormulaTranslator
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.spreadsheet.Address
import com.starburst.starburst.spreadsheet.Cell
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import java.io.ByteArrayOutputStream
import java.time.LocalDate

/**
 * Takes in a list of drivers + items
 * and creates a flat representation which cells at each period
 * thus forming a 2-dimensional sheet of cells similar to a spreadsheet
 *
 * [CellGenerator] does not resolve the dependencies in between cells or populate / validate the expressions
 * the cells will be created with the required references and names only - it's like a skeleton without any of the
 * math
 */
class CellGenerator {

    companion object {

        const val incomeStatementSheetName = "Income Statement"
        const val balanceSheetName = "Balance Sheet"
        const val cashFlowSheetName = "Cash Flow"
        const val otherSheetName = "Other"

        /**
         * Take an financial model and a set of evaluated
         * cells - turn them into XLS based formulas and return the results as
         * a byte array stream
         */
        fun exportToXls(model: Model, cells: List<Cell>): ByteArray {

            val periods = model.periods
            val formulatedCells = addXlsFormulaToCells(cells)

            val wb = SXSSFWorkbook()
            val yearStyle = yearStyle(wb)
            val moneyStyle = moneyStyle(wb)

            fun Sheet.writeHeader(items: List<Item>) {
                val row1 = this.createRow(model.excelRowOffset - 1)
                for (period in 0..periods) {
                    val year = LocalDate.now().year + period
                    val cell = row1
                        .createCell(period + model.excelColumnOffset)
                    cell.setCellValue("FY$year")
                    cell.cellStyle = yearStyle
                }
                items.forEachIndexed { row, item ->
                    this
                        .createRow(row + model.excelRowOffset)
                        .createCell(model.excelColumnOffset - 1)
                        .setCellValue(item.description ?: item.name)
                }
            }

            val sheet1 = wb.createSheet(incomeStatementSheetName)
            sheet1.writeHeader(model.incomeStatementItems)

            val sheet2 = wb.createSheet(balanceSheetName)
            sheet2.writeHeader(model.balanceSheetItems)

            val sheet3 = wb.createSheet(cashFlowSheetName)
            sheet3.writeHeader(model.cashFlowStatementItems)

            val sheet4 = wb.createSheet(otherSheetName)
            sheet4.writeHeader(model.otherItems)

            formulatedCells.forEach { cell ->
                val address = cell.address ?: error("${cell.name}'s address is not found or formulated")
                val sheet = wb.getSheetAt(address.sheet)
                val targetRow = address.row - 1
                val row = if (sheet.getRow(targetRow) == null) {
                    sheet.createRow(targetRow)
                } else {
                    sheet.getRow(targetRow)
                }
                val xlsCell = row.createCell(address.column, CellType.FORMULA)
                xlsCell.cellFormula = cell.excelFormula
                xlsCell.cellStyle = moneyStyle
            }

            return writeWorkbook(wb)
        }

        private fun addXlsFormulaToCells(cells: List<Cell>): List<Cell> {
            val xlsFormulaTranslator = ExcelFormulaTranslator(cells)
            return cells.map { cell ->
                xlsFormulaTranslator.convertCellFormulaToXlsFormula(cell)
            }
        }

        private fun yearStyle(wb: Workbook): CellStyle? {
            val style = wb.createCellStyle()
            val font = wb.createFont()
            font.bold = true
            style.setFont(font)
            style.alignment = HorizontalAlignment.RIGHT
            return style
        }

        private fun writeWorkbook(wb: Workbook): ByteArray {
            val outputStream = ByteArrayOutputStream()
            wb.write(outputStream)
            outputStream.close()
            wb.close()
            return outputStream.toByteArray()
        }

        private fun moneyStyle(wb: Workbook): CellStyle {
            val style = wb.createCellStyle()
            val dataFormat = wb.createDataFormat()
            style.dataFormat = dataFormat.getFormat("_(\$* #,##0.00_);_(\$* (#,##0.00);_(\$* \"-\"??_);_(@_)")
            style.alignment = HorizontalAlignment.RIGHT
            return style
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
                        sheetName = incomeStatementSheetName,
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
                        sheetName = balanceSheetName,
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
                        sheetName = cashFlowSheetName,
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
                        sheetName = otherSheetName,
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
            8 -> "I"
            9 -> "J"
            10 -> "K"
            11 -> "L"
            12 -> "M"
            else -> "N"
        }
    }

}
