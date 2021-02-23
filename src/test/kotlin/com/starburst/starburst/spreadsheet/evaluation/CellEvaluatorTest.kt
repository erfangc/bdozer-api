package com.starburst.starburst.spreadsheet.evaluation

import com.starburst.starburst.models.translator.CellFormulaTranslator
import com.starburst.starburst.models.translator.ModelToCellTranslator
import com.starburst.starburst.models.translator.subtypes.ModelToCellTranslatorTest
import com.starburst.starburst.models.translator.subtypes.CustomDriverFormulaTranslatorTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException

internal class CellEvaluatorTest {

    @Test
    fun evaluate() {
        val model = ModelToCellTranslatorTest.pcCorp()
        val results = CellEvaluator().evaluate(
            cells = CellFormulaTranslator().populateCellsWithFormulas(
                model = model,
                cells = ModelToCellTranslator().generateCells(model)
            )
        )

        Assertions.assertEquals(12, results.size)

        assert(results.any { result -> result.value == 1275000.0 && result.name == "SaaSRevenue_Period1" })
        assert(results.any { result -> result.value == 1800000.0 && result.name == "SaaSRevenue_Period2" })

        assert(results.any { result -> result.value == 318750.0 && result.name == "Salary_Period1" })
        assert(results.any { result -> result.value == 450000.0 && result.name == "Salary_Period2" })

        assert(results.any { result -> result.value == 1000.0 && result.name == "Computers_Period1" })
        assert(results.any { result -> result.value == 1000.0 && result.name == "Computers_Period2" })

    }

    @Test
    internal fun evaluateWithCustomFormula() {
        val model = CustomDriverFormulaTranslatorTest.fakeAircraftCompany()
        val results = CellEvaluator().evaluate(
            cells = CellFormulaTranslator().populateCellsWithFormulas(
                model = model,
                cells = ModelToCellTranslator().generateCells(model)
            )
        )
        Assertions.assertEquals(9, results.size)
        Assertions.assertEquals(143.0, results.find { it.name == "Profit_Period2" }?.value)
    }

    @Test
    internal fun evaluateWithCircularReference() {
        val model = CustomDriverFormulaTranslatorTest.circularReferenceModel()
        val a = assertThrows<IllegalStateException> {
            CellEvaluator().evaluate(
                cells = CellFormulaTranslator().populateCellsWithFormulas(
                    model = model,
                    cells = ModelToCellTranslator().generateCells(model)
                )
            )
        }
        Assertions.assertEquals("Circular dependency found, I1_Period1 -> I3_Period1 -> I2_Period1 -> Bad_Period1", a.message)
    }
}
