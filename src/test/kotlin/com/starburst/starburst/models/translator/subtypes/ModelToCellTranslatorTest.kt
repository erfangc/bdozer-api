package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.models.Driver
import com.starburst.starburst.models.DriverType
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.translator.ModelToCellTranslator
import com.starburst.starburst.models.translator.subtypes.dataclasses.FixedCost
import com.starburst.starburst.models.translator.subtypes.dataclasses.SaaSRevenue
import com.starburst.starburst.models.translator.subtypes.dataclasses.VariableCost
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ModelToCellTranslatorTest {

    @Test
    fun generateCells() {
        val model = pcCorp()

        val results = ModelToCellTranslator().generateCells(model)

        assertEquals(15, results.size)

        assert(results.any { result -> result.name == "Revenue_Period0" })
        assert(results.any { result -> result.name == "Revenue_Period1" })
        assert(results.any { result -> result.name == "Revenue_Period2" })

        assert(results.any { result -> result.name == "SaaSRevenue_Period0" })
        assert(results.any { result -> result.name == "SaaSRevenue_Period1" })
        assert(results.any { result -> result.name == "SaaSRevenue_Period2" })

        assert(results.any { result -> result.name == "Salary_Period0" })
        assert(results.any { result -> result.name == "Salary_Period1" })
        assert(results.any { result -> result.name == "Salary_Period2" })

        assert(results.any { result -> result.name == "Computers_Period0" })
        assert(results.any { result -> result.name == "Computers_Period1" })
        assert(results.any { result -> result.name == "Computers_Period2" })

        assert(results.any { result -> result.name == "COGS_Period0" })
        assert(results.any { result -> result.name == "COGS_Period1" })
        assert(results.any { result -> result.name == "COGS_Period2" })
    }

    companion object {
        fun pcCorp() = Model(
            incomeStatementItems = listOf(
                Item(
                    drivers = listOf(
                        Driver(
                            name = "SaaSRevenue",
                            type = DriverType.SaaSRevenue,
                            saaSRevenue = SaaSRevenue(
                                totalSubscriptionAtTerminalYear = 120_000,
                                initialSubscriptions = 50_000,
                                averageRevenuePerSubscription = 15.0
                            )
                        )
                    ),
                    name = "Revenue"
                ),
                Item(
                    drivers = listOf(
                        Driver(
                            name = "Salary",
                            type = DriverType.VariableCost,
                            variableCost = VariableCost(
                                percentOfRevenue = 0.25
                            )
                        ),
                        Driver(
                            name = "Computers",
                            type = DriverType.FixedCost,
                            fixedCost = FixedCost(
                                cost = 1000.0
                            )
                        )
                    ),
                    name = "COGS"
                )
            ),
            periods = 2
        )

    }
}
