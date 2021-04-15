package com.bdozer.models

import com.bdozer.models.dataclasses.Item
import com.bdozer.models.dataclasses.ItemType
import com.bdozer.models.dataclasses.Model
import com.bdozer.models.translator.FormulaTranslationContext
import com.bdozer.models.translator.subtypes.*
import com.bdozer.spreadsheet.Address
import com.bdozer.spreadsheet.Cell
import com.bdozer.spreadsheet.ExcelFormulaTranslator
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

        private const val incomeStatementSheetName = "Income Statement"
        private const val balanceSheetName = "Balance Sheet"
        private const val cashFlowSheetName = "Cash Flow"
        private const val otherSheetName = "Other"

        /**
         * Take an financial model and a set of evaluated
         * cells - turn them into XLS based formulas and return the results as
         * a byte array stream
         */
        fun exportToXls(model: Model, cells: List<Cell>): ByteArray {

            val periods = model.periods
            val formulatedCells = addXlsFormulaToCells(cells)

            val wb = SXSSFWorkbook()
            /*
            These styles will be reused
             */
            val moneyStyle = moneyStyle(wb)

            /**
             * Helper function to write the non-data cells (i.e. date and item name cells)
             * for each worksheet given it's list of associated items
             */
            fun Sheet.writeHeader(items: List<Item>) {
                val yearStyle = yearStyle(wb)
                val yearRow = this.createRow(model.excelRowOffset - 1)
                for (period in 0..periods) {
                    val year = LocalDate.now().year + period
                    val cell = yearRow
                        .createCell(period + model.excelColumnOffset)
                    cell.setCellValue("FY$year")
                    cell.cellStyle = yearStyle
                }
                items.forEachIndexed { rowNumber, item ->
                    val row = this.createRow(rowNumber + model.excelRowOffset)
                    val labelCell = row.createCell(model.excelColumnOffset - 1)
                    labelCell.setCellValue(item.description ?: item.name)
                    val itemCommentary = item.commentaries?.commentary
                    /*
                    Create a cell comment if the Item being written to the worksheet
                    has a comment stored
                     */
                    if (!itemCommentary.isNullOrBlank()) {
                        /*
                        apparently all of this idiocy is needed for
                        XLS comments, as comments are stored as rich text objects separate from the cell
                        see: http://poi.apache.org/components/spreadsheet/quick-guide.html#CellComments
                         */
                        val factory = wb.creationHelper
                        val anchor = factory.createClientAnchor()

                        anchor.setCol1(labelCell.columnIndex)
                        anchor.setCol2(labelCell.columnIndex + 1)
                        anchor.row1 = row.rowNum
                        anchor.row2 = row.rowNum + 4

                        val drawing = createDrawingPatriarch()
                        val comment = drawing.createCellComment(anchor)
                        comment.string = factory.createRichTextString(itemCommentary)
                        comment.author = "Bot"
                        labelCell.cellComment = comment
                    }
                }
            }

            /*
            create the worksheets for all the statements and the DCF model itself
             */
            val sheet1 = wb.createSheet(incomeStatementSheetName)
            sheet1.defaultColumnWidth = 17
            sheet1.writeHeader(model.incomeStatementItems)

            val sheet2 = wb.createSheet(balanceSheetName)
            sheet2.defaultColumnWidth = 17
            sheet2.writeHeader(model.balanceSheetItems)

            val sheet3 = wb.createSheet(cashFlowSheetName)
            sheet3.defaultColumnWidth = 17
            sheet3.writeHeader(model.cashFlowStatementItems)

            val sheet4 = wb.createSheet(otherSheetName)
            sheet4.defaultColumnWidth = 17
            sheet4.writeHeader(model.otherItems)

            /*
            write the formula of each cell
             */
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
        return populateCellsWithFormulas(
            cells = generateEmptyCells(model),
            model = model
        )
    }

    private fun populateCellsWithFormulas(model: Model, cells: List<Cell>): List<Cell> {

        val ctx = FormulaTranslationContext(model = model, cells = cells)

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
                val historicalValue = item.historicalValue?.value ?: 0.0
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

                    ItemType.FixedCost -> FixedCostTranslator()
                        .translateFormula(cell)

                    ItemType.UnitSalesRevenue -> UnitSalesRevenueTranslator(ctx)
                        .translateFormula(cell)

                    ItemType.Discrete -> DiscreteTranslator(ctx)
                        .translateFormula(cell)

                    ItemType.CompoundedGrowth -> CompoundedGrowthTranslator(ctx)
                        .translateFormula(cell)

                    ItemType.Custom -> CustomTranslator(ctx)
                        .translateFormula(cell)
                }
            }
            updatedCell
        }
    }

    private fun generateEmptyCells(model: Model): List<Cell> {
        val periods = model.periods
        return (0..periods).flatMap { period ->

            /*
            XLS address components preparation based on the current period
            and the item we are working with
             */
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
            this is a special cell inserted at the end of the income statement
            cell groups to provide the ability for cells to reference
            "period"
             */
            val periodCell = Cell(
                period = period,
                item = Item(name = "period", formula = "$period"),
                name = "period_$period",
                value = period.toDouble(),
                address = Address(
                    sheet = 0,
                    sheetName = incomeStatementSheetName,
                    row = idxToRow(incomeStatementCells.size),
                    column = column,
                    columnLetter = columnLetter
                )
            )

            /*
            create the balance sheet cells
             */
            val balanceSheetCells = model.balanceSheetItems.mapIndexed { idx, item ->
                Cell(
                    name = "${item.name}_Period$period",
                    formula = item.formula,
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
                    formula = item.formula,
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
                    formula = item.formula,
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

            listOf(periodCell) + incomeStatementCells + balanceSheetCells + cashFlowStatementItems + otherCells
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
