package com.starburst.starburst.computers

import com.starburst.starburst.computers.CellGeneratorTest.Companion.model
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class CellEvaluatorTest {

    @Test
    fun evaluate() {
        val model = model()
        val results = CellEvaluator().evaluate(
            model = model,
            cells = CellExperssionResolver().resolveCellExpressions(
                model = model,
                cells = CellGenerator().generateCells(model)
            )
        )

        assertEquals(4, results.size)
        assert(results.any { result -> result.value == 1275000.0 && result.name == "Revenue_Period1" })
        assert(results.any { result -> result.value == 1800000.0 && result.name == "Revenue_Period2" })
        assert(results.any { result -> result.value == 318750.0 && result.name == "Salary_Period1" })
        assert(results.any { result -> result.value == 450000.0 && result.name == "Salary_Period2" })

    }
}
