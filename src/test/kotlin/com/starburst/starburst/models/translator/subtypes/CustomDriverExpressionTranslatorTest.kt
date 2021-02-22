package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.models.translator.CellFormulaTranslator
import com.starburst.starburst.models.translator.ModelToCellTranslator
import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.models.DriverType
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.translator.subtypes.dataclasses.CustomDriver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CustomDriverExpressionTranslatorTest {

    @Test
    fun resolveExpression() {
        val model = fakeAircraftCompany()

        val cells = CellFormulaTranslator()
            .populateCellsWithFormulas(
                model, ModelToCellTranslator().generateCells(model = model)
            )

        val ctx = ResolverContext(model = model, cells = cells)

        val results = CustomExpressionTranslator(ctx)
            .translateFormula(cells.find { it.name == "Aircraft_Parts_Period2" }!!)

        assertEquals("Aircraft_Revenue_Period2*0.35", results.formula)
    }

    companion object {
        fun fakeAircraftCompany() = Model(
            incomeStatementItems = listOf(
                Item(
                    name = "Aircraft_Revenue",
                    expression = "(100% + period * 5%) * 200"
                ),
                Item(
                    name = "Aircraft_Parts",
                    expression = "Aircraft_Revenue * 0.35",
                ),
                Item(
                    expression = "Aircraft_Revenue - Aircraft_Parts",
                    name = "Profit"
                )
            ),
            periods = 2
        )

        fun circularReferenceModel() = Model(
            incomeStatementItems = listOf(
                Item(
                    name = "I1",
                    type = DriverType.Custom,
                    expression = "10 + Bad"
                ),
                Item(
                    name = "I2",
                    type = DriverType.Custom,
                    expression = "I3 / 10"
                ),
                Item(
                    expression = "I1 * I2",
                    name = "I3"
                ),
                Item(
                    expression = "I2 + 100.0",
                    name = "Bad"
                )
            ),
            periods = 3
        )

    }
}
