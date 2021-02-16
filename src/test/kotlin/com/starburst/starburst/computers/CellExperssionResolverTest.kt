package com.starburst.starburst.computers

import com.starburst.starburst.computers.CellGeneratorTest.Companion.model
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class CellExperssionResolverTest {

    @Test
    fun resolveCellExpressions() {
        val model = model()
        val results = CellExperssionResolver().resolveCellExpressions(
            model,
            CellGenerator().generateCells(model)
        )

        Assertions.assertEquals(4, results.size)

        assert(results.any { result -> result.expression == "((((120000 - 50000) / 2) * 1) + 50000) * 15.0" && result.name == "Revenue_Period1" })
        assert(results.any { result -> result.expression == "((((120000 - 50000) / 2) * 2) + 50000) * 15.0" && result.name == "Revenue_Period2" })
        assert(results.any { result -> result.expression == "Revenue_Period1 * 0.25" && result.name == "Salary_Period1" })
        assert(results.any { result -> result.expression == "Revenue_Period2 * 0.25" && result.name == "Salary_Period2" })

    }
}
