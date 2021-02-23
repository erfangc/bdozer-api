package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.computers.ReservedItemNames.Revenue
import com.starburst.starburst.models.ItemType
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.translator.ModelToCellTranslator
import com.starburst.starburst.models.translator.subtypes.dataclasses.FixedCost
import com.starburst.starburst.models.translator.subtypes.dataclasses.SubscriptionRevenue
import com.starburst.starburst.models.translator.subtypes.dataclasses.PercentOfRevenue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ModelToCellTranslatorTest {

    @Test
    fun generateCells() {
        val model = pcCorp()

        val results = ModelToCellTranslator().generateCells(model)

        assertEquals(12, results.size)

        assert(results.any { result -> result.name == "SaaSRevenue_Period0" })
        assert(results.any { result -> result.name == "SaaSRevenue_Period1" })
        assert(results.any { result -> result.name == "SaaSRevenue_Period2" })

        assert(results.any { result -> result.name == "Salary_Period0" })
        assert(results.any { result -> result.name == "Salary_Period1" })
        assert(results.any { result -> result.name == "Salary_Period2" })

        assert(results.any { result -> result.name == "Computers_Period0" })
        assert(results.any { result -> result.name == "Computers_Period1" })
        assert(results.any { result -> result.name == "Computers_Period2" })

    }

    companion object {
        fun pcCorp() = Model(
            incomeStatementItems = listOf(
                Item(
                    name = "SaaSRevenue",
                    type = ItemType.SubscriptionRevenue,
                    subscriptionRevenue = SubscriptionRevenue(
                        totalSubscriptionAtTerminalYear = 120_000,
                        initialSubscriptions = 50_000,
                        averageRevenuePerSubscription = 15.0
                    )
                ),
                Item(
                    name = Revenue,
                    expression = "SaaSRevenue"
                ),
                Item(
                    name = "Computers",
                    type = ItemType.FixedCost,
                    fixedCost = FixedCost(
                        cost = 1000.0
                    )
                ),
                Item(
                    name = "Salary",
                    type = ItemType.PercentOfTotalAsset,
                    percentOfRevenue = PercentOfRevenue(
                        percentOfRevenue = 0.25
                    )
                )
            ),
            periods = 2
        )

    }
}
