package com.starburst.starburst.models.translator.resolvers

import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.cells.Cell
import com.starburst.starburst.models.Item
import org.mariuszgromada.math.mxparser.Expression
import org.mariuszgromada.math.mxparser.parsertokens.Token.NOT_MATCHED

/**
 * [StringExpressionResolver] resolves a [Cell] whose value is linked to an [Item] with [Item.expression] populated.
 * Normally [Item]'s expression is the sum of drivers - [Item] may take on value specified by [Item.expression]. This resolver
 * populates such expression with real cells
 *
 */
class StringExpressionResolver(ctx: ResolverContext) : ExpressionResolver {

    //
    // create a lookup dictionary of item/driver names -> cell names
    //
    private val lookup = ctx.cells.groupBy { it.period }
        .mapValues { entry -> entry.value.associateBy { it.driver?.name ?: it.item?.name ?: error("") } }

    /**
     * The primary job is to tokenize the expression of a given [Item] and replace
     * the generic tokens with actual cell names as well as populate the dependency tree
     */
    override fun resolveExpression(cell: Cell): Cell {
        val origEl = Expression(cell.expression)

        val tokens = mutableListOf<String>()
        val dependentCellNames = mutableListOf<String>()

        //
        // for every token, if its unmatched, then try to match it with a cell name
        //
        origEl.copyOfInitialTokens.forEach { token ->
            val tokenStr = token.tokenStr
            if (token.tokenTypeId == NOT_MATCHED) {
                val dependentCell = lookup[cell.period]?.get(tokenStr)
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
            expression = tokens.joinToString(""),
            dependentCellNames = dependentCellNames.toList() // make the list immutable again
        )
    }
}
