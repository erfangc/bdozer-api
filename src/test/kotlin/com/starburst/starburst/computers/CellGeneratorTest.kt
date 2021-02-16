package com.starburst.starburst.computers

import com.starburst.starburst.computers.drivers.SaaSRevenue
import com.starburst.starburst.models.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CellGeneratorTest {

    @Test
    fun generateCells() {
        val model = model()

        val results = CellGenerator().generateCells(model)

        assertEquals(4, results.size)
        assert(results.any { result -> result.name == "Revenue_Period1" })
        assert(results.any { result -> result.name == "Revenue_Period2" })
        assert(results.any { result -> result.name == "Salary_Period1" })
        assert(results.any { result -> result.name == "Salary_Period2" })
    }

    companion object {
        fun model() = Model(
            drivers = listOf(
                Driver(
                    name = "Revenue",
                    type = DriverType.SaaSRevenue,
                    saaSRevenue = SaaSRevenue(
                        totalSubscriptionAtTerminalYear = 120_000,
                        initialSubscriptions = 50_000,
                        averageRevenuePerSubscription = 15.0
                    )
                ),
                Driver(
                    name = "Salary",
                    type = DriverType.VariableCost,
                    variableCost = VariableCost(
                        percentOfRevenue = 0.25
                    )
                )
            ),
            periods = 2
        )

    }
}
