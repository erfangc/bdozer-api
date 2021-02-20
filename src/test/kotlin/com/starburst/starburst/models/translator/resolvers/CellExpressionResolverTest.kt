package com.starburst.starburst.models.translator.resolvers

import com.starburst.starburst.models.translator.resolvers.ModelToCellTranslatorTest.Companion.pcCorp
import com.starburst.starburst.models.translator.CellExpressionResolver
import com.starburst.starburst.models.translator.ModelToCellTranslator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class CellExpressionResolverTest {

    @Test
    fun resolveCellExpressions() {
        val model = pcCorp()
        val results = CellExpressionResolver().resolveCellExpressions(
            model,
            ModelToCellTranslator().generateCells(model)
        )

        Assertions.assertEquals(10, results.size)

        assert(results.any { result -> result.expression == "((((120000 - 50000) / 2) * 1) + 50000) * 15.0" && result.name == "SaaSRevenue_Period1" })
        assert(results.any { result -> result.expression == "((((120000 - 50000) / 2) * 2) + 50000) * 15.0" && result.name == "SaaSRevenue_Period2" })

        assert(results.any { result -> result.expression == "SaaSRevenue_Period1" && result.name == "Revenue_Period1" })
        assert(results.any { result -> result.expression == "SaaSRevenue_Period2" && result.name == "Revenue_Period2" })

        assert(results.any { result -> result.expression == "Revenue_Period1 * 0.25" && result.name == "Salary_Period1" })
        assert(results.any { result -> result.expression == "Revenue_Period2 * 0.25" && result.name == "Salary_Period2" })

        assert(results.any { result -> result.expression == "1000.0" && result.name == "Computers_Period1" })
        assert(results.any { result -> result.expression == "1000.0" && result.name == "Computers_Period2" })

        assert(results.any { result -> result.expression == "Salary_Period1+Computers_Period1" && result.name == "COGS_Period1" })
        assert(results.any { result -> result.expression == "Salary_Period2+Computers_Period2" && result.name == "COGS_Period2" })

    }
}
