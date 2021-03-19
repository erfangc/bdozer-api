package com.starburst.starburst.spreadsheet.evaluation

import com.starburst.starburst.spreadsheet.Cell
import org.mariuszgromada.math.mxparser.Argument
import org.mariuszgromada.math.mxparser.Expression
import org.slf4j.LoggerFactory
import java.util.*

/**
 * For ever cell to be evaluated, use a depth-first approach to evaluate its value
 * that means all dependencies are evaluated ahead of the actual cell
 */
class CellEvaluator {

    private val log = LoggerFactory.getLogger(CellEvaluator::class.java)

    /**
     * Takes as input a list of cells that is pure formula
     * and output those same cells with values populated
     */
    fun evaluate(cells: List<Cell>): List<Cell> {

        var countCalculate = 0
        val start = System.currentTimeMillis()
        var timeSpentCalculating = 0L
        var timeSpentCheckingCircularDependency = 0L

        // we cannot reference in this method Item/Driver - because domain specific business logic
        // must be handled prior to this step, otherwise it becomes very easy to conflate
        // the evaluation of this DAG of cells with concepts that are rooted in the nature of modeling
        // such as "how to depreciate an asset" etc.
        // TODO make a defensive copy of the cells without referencing Item/Driver

        val stack = Stack<Cell>()

        //
        // create lookup map of all cells by their name
        // as a mutable map, so we can update the cells in place
        // as they are being evaluated
        //
        val cellLookupByName = cells.associateBy { it.name }.toMutableMap()

        /**
         * helper function to determine the unmet dependencies of any given cell
         * this requires the loop up
         */
        fun unmetDependencies(cell: Cell): List<Cell> {
            return cell.dependentCellNames.map { dep ->
                cellLookupByName[dep] ?: error("referenced cell $dep does not exist")
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
                if (headCell.value != null) {
                    stack.pop()
                    continue
                }
                val unmetDependencies = unmetDependencies(headCell)
                if (unmetDependencies.isEmpty()) {
                    // evaluate the cell
                    // using mXparser
                    val value = try {
                        val expression = Expression(headCell.formula)
                        // add some common arguments
                        expression.addArguments(Argument("period", headCell.period.toDouble()))
                        expression.missingUserDefinedArguments.forEach { argName ->
                            val argValue = cellLookupByName[argName]?.value ?: error("unable to resolve $argName")
                            expression.addArguments(
                                Argument(argName, argValue)
                            )
                        }
                        countCalculate++
                        val s = System.currentTimeMillis()
                        val result = expression.calculate()
                        val e = System.currentTimeMillis()
                        timeSpentCalculating += e - s
                        result
                    } catch (e: Exception) {
                        log.error("Unable to evaluate cell ${cell.name}", e)
                        0.0
                    }
                    //
                    // update the cached source of cell values
                    //
                    cellLookupByName[headCell.name] = headCell.copy(value = value)
                    stack.pop()
                } else {
                    // push dependencies into the stack, so they will now be evaluated
                    unmetDependencies.forEach { dependentCell ->
                        stack.push(dependentCell)
                        // circular dependency handling code
                        val s = System.currentTimeMillis()
                        checkCircularReference(stack, headCell)
                        val e = System.currentTimeMillis()
                        timeSpentCheckingCircularDependency += e - s
                    }
                }
            }
        }
        log.info(
            "numberOfCalculations=$countCalculate, " +
                    "cells.size=${cells.size}, " +
                    "totalCalculationTime=${System.currentTimeMillis() - start}ms, " +
                    "timeSpentCalculating=${timeSpentCalculating}ms, " +
                    "timeSpentCheckingCircularDependency=${timeSpentCheckingCircularDependency}ms, "
        )
        return cells.map { cell -> cellLookupByName[cell.name] ?: error("...") }
    }

    private fun checkCircularReference(stack: Stack<Cell>, headCell: Cell) {
        // TODO get this to become better
        val lst = stack.toList()
        val withoutHeadCell = lst.subList(1, lst.size)
        val circularReferenceDetected = withoutHeadCell.contains(headCell)

        if (circularReferenceDetected) {
            // we trace the circular reference back to it's source
            // and print this chain - by traversing the stack as if it's a list in reverse
            val chain = mutableListOf(headCell)
            var currCell = stack.pop()
            while (currCell != headCell) {
                chain.add(currCell)
                currCell = stack.pop()
            }
            val chainStr = chain.joinToString(" -> ") { it.name }
            error("Circular dependency found, $chainStr")
        }
    }

}
