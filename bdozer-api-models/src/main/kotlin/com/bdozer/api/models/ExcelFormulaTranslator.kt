package com.bdozer.api.models

import com.bdozer.api.models.dataclasses.spreadsheet.Cell
import org.mariuszgromada.math.mxparser.Expression
import org.mariuszgromada.math.mxparser.parsertokens.Token

/**
 * This class can take in a list of cells (which may or may not be renderable across multiple Sheet(s) within the same work book)
 * and replace their in-memory JVM computation formula with Excel formula using Excel addresses
 *
 * For example:
 *
 * `Revenue_Period1 - CostOfGoods_Period1`
 *
 * becomes
 *
 * `'IncomeStatement'!B2 - 'IncomeStatement'!B3`
 *
 * The way this is done is that: we re-tokenize each formula expression. For any symbol we encounter
 * that are unknown - i.e. not a built-in operator like +/-/if() etc. - instead of replacing this unknown token
 * with the name of the referenced [Cell], we simply replace the token with the referenced cell's
 * Excel address instead
 *
 * Excel cell address is specified on the referenced [Cell]
 */
class ExcelFormulaTranslator(private val cells: List<Cell>) {

    /**
     * The primary job is to tokenize the expression of a given [Cell] and replace
     * the generic tokens with actual cell names as well as populate the dependency tree
     */
    fun convertCellFormulaToXlsFormula(cell: Cell): Cell {

        //
        // create a lookup dictionary of item/driver names -> cell names
        // the first layer of the lookup is by period, the second layer of the map
        // is by name
        //
        val lookup = cells.associateBy { it.name }

        val origEl = Expression(cell.formula)

        val tokens = mutableListOf<String>()
        val dependentCellNames = mutableListOf<String>()

        //
        // for every token, if its unmatched, then try to match it with a cell name
        //
        origEl.copyOfInitialTokens.forEach { token ->
            val tokenStr = token.tokenStr
            if (token.tokenTypeId == Token.NOT_MATCHED) {
                //
                // create a library of cells that can be referenced by the current cell
                //
                val dependentCell = lookup[tokenStr]
                if (dependentCell != null) {
                    dependentCellNames.add(dependentCell.name)
                    //
                    // figure out the Excel cell address of the dependent cell
                    //
                    val address = dependentCell.address ?: error("...")
                    val cellAddressStr = "'${address.sheetName}'!${address.columnLetter}${address.row}"
                    tokens.add(cellAddressStr)
                } else {
                    // we've encountered an argument that isn't a cell value, so do not transform it
                    tokens.add(tokenStr)
                }
            } else {
                // we encountered a normal token, just add it back
                tokens.add(tokenStr)
            }

        }
        return cell.copy(
            excelFormula = tokens.joinToString("")
        )
    }
}
