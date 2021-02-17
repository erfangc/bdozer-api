package com.starburst.starburst.computers

import com.starburst.starburst.computers.CellGeneratorTest.Companion.pcCorp
import com.starburst.starburst.computers.expression.resolvers.CustomExpressionResolverTest.Companion.circularReferenceModel
import com.starburst.starburst.computers.expression.resolvers.CustomExpressionResolverTest.Companion.fakeAircraftCompany
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException

internal class CellEvaluatorTest {

    @Test
    fun evaluate() {
        val model = pcCorp()
        val results = CellEvaluator().evaluate(
            model = model,
            cells = CellExpressionResolver().resolveCellExpressions(
                model = model,
                cells = CellGenerator().generateCells(model)
            )
        )

        assertEquals(10, results.size)

        assert(results.any { result -> result.value == 1275000.0 && result.name == "SaaSRevenue_Period1" })
        assert(results.any { result -> result.value == 1800000.0 && result.name == "SaaSRevenue_Period2" })

        assert(results.any { result -> result.value == 1275000.0 && result.name == "Revenue_Period1" })
        assert(results.any { result -> result.value == 1800000.0 && result.name == "Revenue_Period2" })

        assert(results.any { result -> result.value == 318750.0 && result.name == "Salary_Period1" })
        assert(results.any { result -> result.value == 450000.0 && result.name == "Salary_Period2" })

        assert(results.any { result -> result.value == 1000.0 && result.name == "Computers_Period1" })
        assert(results.any { result -> result.value == 1000.0 && result.name == "Computers_Period2" })

        assert(results.any { result -> result.value == 319750.0 && result.name == "COGS_Period1" })
        assert(results.any { result -> result.value == 451000.0 && result.name == "COGS_Period2" })

    }

    @Test
    internal fun evaluateWithCustomFormula() {
        val model = fakeAircraftCompany()
        val results = CellEvaluator().evaluate(
            model = model,
            cells = CellExpressionResolver().resolveCellExpressions(
                model = model,
                cells = CellGenerator().generateCells(model)
            )
        )
        assertEquals(8, results.size)
        assertEquals(143.0, results.find { it.name == "Profit_Period2" }?.value)
    }

    @Test
    internal fun evaluateWithCircularReference() {
        val model = circularReferenceModel()
        val a = assertThrows<IllegalStateException> {
            CellEvaluator().evaluate(
                model = model,
                cells = CellExpressionResolver().resolveCellExpressions(
                    model = model,
                    cells = CellGenerator().generateCells(model)
                )
            )
        }
        assertEquals("Circular dependency found, D1_Period1 -> I2_Period1 -> Bad_Period1", a.message)
    }
}
