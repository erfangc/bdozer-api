package com.starburst.starburst.models.translator

import com.starburst.starburst.models.ResolverContext
import com.starburst.starburst.models.translator.subtypes.FormulaTranslator
import com.starburst.starburst.spreadsheet.Cell
import org.mariuszgromada.math.mxparser.Expression
import org.mariuszgromada.math.mxparser.parsertokens.Token


/**
 * [GenericTranslator] resolves a [Cell] whose value is linked to an [Item] with [Item.expression] populated.
 * Normally [Item]'s expression is the sum of drivers - [Item] may take on value specified by [Item.expression]. This resolver
 * populates such expression with real cells
 *
 */
class ExcelFormulaTranslator(ctx: ResolverContext) : FormulaTranslator {

    //
    // create a lookup dictionary of item/driver names -> cell names
    // the first layer of the lookup is by period, the second layer of the map
    // is by name
    //
    private val lookup = ctx.cells.associateBy { it.name }

    /**
     * The primary job is to tokenize the expression of a given [Item] and replace
     * the generic tokens with actual cell names as well as populate the dependency tree
     */
    override fun translateFormula(cell: Cell): Cell {
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
                    val cellAddressStr = "Sheet${address.sheet}!${address.columnLetter}${address.row}"
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
