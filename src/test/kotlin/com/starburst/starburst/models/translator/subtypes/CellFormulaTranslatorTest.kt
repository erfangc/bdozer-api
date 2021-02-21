package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.models.translator.subtypes.ModelToCellTranslatorTest.Companion.pcCorp
import com.starburst.starburst.models.translator.CellFormulaTranslator
import com.starburst.starburst.models.translator.ModelToCellTranslator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class CellFormulaTranslatorTest {

    @Test
    fun resolveCellExpressions() {
        val model = pcCorp()
        val results = CellFormulaTranslator().populateCellsWithFormulas(
            model,
            ModelToCellTranslator().generateCells(model)
        )

        Assertions.assertEquals(15, results.size)

        assert(results.any { result -> result.formula == "0.0" && result.name == "SaaSRevenue_Period0" })
        assert(results.any { result -> result.formula == "((((120000 - 50000) / 2) * 1) + 50000) * 15.0" && result.name == "SaaSRevenue_Period1" })
        assert(results.any { result -> result.formula == "((((120000 - 50000) / 2) * 2) + 50000) * 15.0" && result.name == "SaaSRevenue_Period2" })

        assert(results.any { result -> result.formula == "0.0" && result.name == "Revenue_Period0" })
        assert(results.any { result -> result.formula == "SaaSRevenue_Period1" && result.name == "Revenue_Period1" })
        assert(results.any { result -> result.formula == "SaaSRevenue_Period2" && result.name == "Revenue_Period2" })

        assert(results.any { result -> result.formula == "Revenue_Period1 * 0.25" && result.name == "Salary_Period1" })
        assert(results.any { result -> result.formula == "Revenue_Period2 * 0.25" && result.name == "Salary_Period2" })

        assert(results.any { result -> result.formula == "1000.0" && result.name == "Computers_Period1" })
        assert(results.any { result -> result.formula == "1000.0" && result.name == "Computers_Period2" })

        assert(results.any { result -> result.formula == "Salary_Period1+Computers_Period1" && result.name == "COGS_Period1" })
        assert(results.any { result -> result.formula == "Salary_Period2+Computers_Period2" && result.name == "COGS_Period2" })

    }
}
