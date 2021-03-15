package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.models.Utility.previous
import com.starburst.starburst.spreadsheet.Cell
import com.starburst.starburst.models.translator.FormulaTranslationContext
import com.starburst.starburst.models.dataclasses.Item
import org.mariuszgromada.math.mxparser.Expression
import org.mariuszgromada.math.mxparser.parsertokens.Token.NOT_MATCHED

/**
 * [GenericTranslator] resolves a [Cell] whose value is linked to an [Item] with [Item.expression] populated.
 * Normally [Item]'s expression is the sum of drivers - [Item] may take on value specified by [Item.expression]. This resolver
 * populates such expression with real cells
 *
 */
class GenericTranslator(ctx: FormulaTranslationContext) : FormulaTranslator {

    //
    // create a lookup dictionary of item/driver names -> cell names
    // the first layer of the lookup is by period, the second layer of the map
    // is by name
    //
    private val lookup = ctx.cells.groupBy { it.period }
        .mapValues { entry -> entry.value.associateBy { it.item.name } }

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
            if (token.tokenTypeId == NOT_MATCHED) {
                //
                // create a library of cells that can be referenced by the current cell
                //
                val period = cell.period

                val currentPeriodCells = lookup[period] ?: emptyMap()
                val previousPeriodCells = lookup[period - 1]
                    ?.map { (key, value) ->
                        // this steps enable formulas to reference Previous_<ItemName>
                        // to be possible
                        // note that - for the case where 'period = 1' we must use historical value
                        // which means the cell generator must've created these
                        previous(key) to value
                    }
                    ?.toMap() ?: emptyMap()

                val cellLibrary =  currentPeriodCells + previousPeriodCells

                val dependentCell = cellLibrary[tokenStr]
                if (dependentCell != null) {
                    dependentCellNames.add(dependentCell.name)
                    tokens.add(dependentCell.name)
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
            formula = tokens.joinToString(""),
            dependentCellNames = dependentCellNames.toList() // make the list immutable again
        )
    }
}
