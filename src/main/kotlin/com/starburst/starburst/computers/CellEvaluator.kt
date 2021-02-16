package com.starburst.starburst.computers

import com.starburst.starburst.models.Cell
import com.starburst.starburst.models.Model
import org.slf4j.LoggerFactory
import java.util.*
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import javax.script.SimpleBindings

/**
 * For ever cell to be evaluated, use a depth-first approach to evaluate its value
 * that means all dependencies are evaluated ahead of the actual cell
 */
class CellEvaluator {

    private val engine = ScriptEngineManager().getEngineByExtension("js")
    private val logger = LoggerFactory.getLogger(CellEvaluator::class.java)

    fun evaluate(
        model: Model,
        cells: List<Cell>
    ): List<Cell> {

        val stack = Stack<Cell>()

        //
        // create lookup map of all cells by their name
        // as a mutable map, so we can update the cells in place
        // as they are being evaluated
        //
        val cellLookup = cells.associateBy { it.name }.toMutableMap()

        /**
         * helper function to determine the unmet dependencies of any given cell
         * this requires the loop up
         */
        fun unmetDependencies(cell: Cell): List<Cell> {
            return cell.dependencies.map { dep ->
                cellLookup[dep]!!
            }.filter { it.value == null }
        }

        cells.forEach { cell ->

            if (stack.isNotEmpty()) {
                error("this should never happen?")
            }

            // put the current cell onto the stack - if it has no dependencies it will be popped in the next
            // step and evaluated
            stack.push(cell)

            // try to evaluate the current cell -
            // if it's dependencies have not been met then push the unmet dependencies into a stack
            // to be evaluated ahead of it
            while (stack.isNotEmpty()) {

                // attempt to evaluate the first thing on the stack and pop it off
                // if that item has unresolved dependencies then push those
                // onto the stack for evaluation
                val headCell = stack.peek()

                // if the cell is already evaluated, skip this iteration of the loop
                if (headCell.value !== null) {
                    stack.pop()
                    continue
                }

                val unmetDependencies = unmetDependencies(headCell)

                if (unmetDependencies.isEmpty()) {
                    // evaluate the cell
                    val simpleBindings = SimpleBindings()
                    headCell.dependencies.forEach { dep ->
                        simpleBindings[dep] = cellLookup[dep]?.value
                    }

                    val value = try {
                        engine.eval(headCell.expression, simpleBindings).toString().toDoubleOrNull() ?: 0.0
                    } catch (e: ScriptException) {
                        logger.error("Unable to evaluate cell value", e)
                        0.0
                    }

                    cellLookup[headCell.name] = headCell.copy(value = value)
                    stack.pop()
                } else {
                    // push dependencies
                    unmetDependencies.forEach { dependentCell -> stack.push(dependentCell) }
                    // TODO check circular reference
                }
            }
        }
        return cells.map { cell -> cellLookup[cell.name]!! }
    }

}
