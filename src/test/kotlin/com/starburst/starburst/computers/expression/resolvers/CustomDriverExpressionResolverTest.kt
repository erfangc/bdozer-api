package com.starburst.starburst.computers.expression.resolvers

import com.starburst.starburst.computers.CellExpressionResolver
import com.starburst.starburst.computers.ModelToCellTranslator
import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.models.Driver
import com.starburst.starburst.models.DriverType
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CustomDriverExpressionResolverTest {

    @Test
    fun resolveExpression() {
        val model = fakeAircraftCompany()

        val cells = CellExpressionResolver()
            .resolveCellExpressions(
                model, ModelToCellTranslator().generateCells(model = model)
            )

        val ctx = ResolverContext(model = model, cells = cells)

        val results = CustomExpressionResolver(ctx)
            .resolveExpression(cells.find { it.name == "Aircraft_Parts_Period2" }!!)

        assertEquals("Aircraft_Revenue_Period2*0.35", results.expression)
    }

    companion object {
        fun fakeAircraftCompany() = Model(
            incomeStatementItems = listOf(
                Item(
                    drivers = listOf(
                        Driver(
                            name = "Aircraft_Revenue",
                            customDriver = CustomDriver(formula = "(100% + period * 5%) * 200"),
                            type = DriverType.Custom
                        ),
                        Driver(
                            name = "Aircraft_Parts",
                            customDriver = CustomDriver(formula = "Aircraft_Revenue * 0.35"),
                            type = DriverType.Custom
                        )
                    ),
                    name = "Income_Statement"
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
                    drivers = listOf(
                        Driver(
                            name = "D1",
                            customDriver = CustomDriver(formula = "10 + Bad"),
                            type = DriverType.Custom
                        ),
                        Driver(
                            name = "D2",
                            customDriver = CustomDriver(formula = "D1 / 10"),
                            type = DriverType.Custom
                        )
                    ),
                    name = "I1"
                ),
                Item(
                    expression = "D1 * D2",
                    name = "I2"
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
