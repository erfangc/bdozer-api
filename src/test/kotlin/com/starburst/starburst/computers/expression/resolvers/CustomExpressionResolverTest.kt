package com.starburst.starburst.computers.expression.resolvers

import com.starburst.starburst.computers.CellExpressionResolver
import com.starburst.starburst.computers.CellGenerator
import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.models.Driver
import com.starburst.starburst.models.DriverType
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CustomExpressionResolverTest {

    @Test
    fun resolveExpression() {
        val model = fakeAircraftCompany()

        val cells = CellExpressionResolver()
            .resolveCellExpressions(
                model, CellGenerator().generateCells(model = model)
            )

        val ctx = ResolverContext(model = model, cells = cells)

        val results = CustomExpressionResolver(ctx)
            .resolveExpression(cells.find { it.name == "Aircraft_Parts_Period2" }!!)

        assertEquals("Aircraft_Revenue_Period2*0.35", results.expression)
    }

    companion object {
        fun fakeAircraftCompany() = Model(
            items = listOf(
                Item(
                    drivers = listOf(
                        Driver(
                            name = "Aircraft_Revenue",
                            custom = Custom(expression = "(100% + period * 5%) * 200"),
                            type = DriverType.Custom
                        ),
                        Driver(
                            name = "Aircraft_Parts",
                            custom = Custom(expression = "Aircraft_Revenue * 0.35"),
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
            items = listOf(
                Item(
                    drivers = listOf(
                        Driver(
                            name = "D1",
                            custom = Custom(expression = "10 + Bad"),
                            type = DriverType.Custom
                        ),
                        Driver(
                            name = "D2",
                            custom = Custom(expression = "D1 / 10"),
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
