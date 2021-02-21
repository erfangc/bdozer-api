package com.starburst.starburst.models.builders

import com.starburst.starburst.computers.ReservedItemNames
import com.starburst.starburst.models.Driver
import com.starburst.starburst.models.DriverType
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.translator.subtypes.SaaSRevenue
import com.starburst.starburst.models.translator.subtypes.VariableCost
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ModelBuilderTest {

    /**
     * Full unit test of a fictitious
     */
    @Test
    internal fun reformulateRealisticModel() {
        val model = fictitiousSaaSCompany()
        val modelBuilder = ModelBuilder()
        val reformulated = modelBuilder.reformulateModel(model)

        val cells = modelBuilder.evaluateModel(reformulated)

        val expected = reformulated.incomeStatementItems.size +
                reformulated.balanceSheetItems.size +
                reformulated.otherItems.size +
                2 // +2 for the driver

        val actual = cells.size / (model.periods + 1)

        assertEquals(expected, actual)
    }

    private fun fictitiousSaaSCompany() = Model(
        incomeStatementItems = listOf(
            Item(
                name = "Cloud_Hosting_Revenue",
                drivers = listOf(
                    Driver(
                        name = "Subscription",
                        type = DriverType.SaaSRevenue,
                        saaSRevenue = SaaSRevenue(
                            totalSubscriptionAtTerminalYear = 100_000,
                            initialSubscriptions = 1_000,
                            averageRevenuePerSubscription = 120.0
                        )
                    )
                )
            ),
            Item(
                name = ReservedItemNames.Revenue
            ),
            Item(
                name = "Cloud_Hosting_COGS",
                drivers = listOf(
                    Driver(
                        name = "Equipment",
                        type = DriverType.VariableCost,
                        variableCost = VariableCost(
                            percentOfRevenue = 0.2
                        )
                    )
                )
            ),
            Item(
                name = ReservedItemNames.CostOfGoodsSold,
            ),

            Item(
                name = ReservedItemNames.GrossProfit
            ),
            Item(
                name = "ResearchAndDevelopment",
                expression = "Revenue * 0.14"
            ),
            Item(
                name = "SGA",
                expression = "100000"
            ),
            Item(
                name = ReservedItemNames.OperatingExpense
            ),
            Item(
                name = ReservedItemNames.OperatingIncome
            ),
            Item(
                name = ReservedItemNames.NonOperatingExpense
            ),
            Item(
                name = ReservedItemNames.InterestExpense
            ),
            Item(
                name = ReservedItemNames.TaxExpense
            ),
            Item(
                name = ReservedItemNames.NetIncome
            )
        ),
        balanceSheetItems = listOf(
            Item(
                name = ReservedItemNames.CurrentAsset,
                historicalValue = 1_000_000.0
            ),
            Item(
                name = ReservedItemNames.LongTermAsset,
                historicalValue = 12_000_000.0
            ),
            Item(
                name = ReservedItemNames.TotalAsset,
                historicalValue = 13_000_000.0,
                expression = "${ReservedItemNames.CurrentAsset}+${ReservedItemNames.LongTermAsset}"
            ),
            Item(
                name = ReservedItemNames.CurrentLiability,
                historicalValue = 500_000.0
            ),
            Item(
                name = ReservedItemNames.LongTermLiability,
                historicalValue = 3_500_000.0
            ),
            Item(
                name = ReservedItemNames.TotalLiability,
                historicalValue = 4_000_000.0,
                expression = "${ReservedItemNames.CurrentLiability}+${ReservedItemNames.LongTermLiability}"
            ),
            Item(
                name = ReservedItemNames.ShareholdersEquity,
                historicalValue = 9_000_000.0,
                expression = "${ReservedItemNames.TotalAsset}-${ReservedItemNames.TotalLiability}"
            )
        )
    )

    @Test
    fun reformulateModel() {
        val modelBuilder = ModelBuilder()

        val model = modelBuilder.createModel()

        val updatedModel = model.copy(
            incomeStatementItems = listOf(
                Item(
                    name = "CommercialAircraft",
                    expression = "100.0"
                ),
                Item(
                    name = "MilitaryAircraft",
                    expression = "300.0"
                ),
                Item(
                    name = "ConsultingServices",
                    expression = "200.0"
                ),
                Item(
                    name = ReservedItemNames.Revenue,
                    expression = "0.0"
                ),
                Item(
                    name = "CommercialAircraftCOGS",
                    expression = "50.0"
                ),
                Item(
                    name = "MilitaryAircraftCOGS",
                    expression = "300.0"
                ),
                Item(
                    name = ReservedItemNames.CostOfGoodsSold,
                    expression = "0.0"
                ),
                Item(
                    name = ReservedItemNames.GrossProfit,
                    expression = "${ReservedItemNames.Revenue} - ${ReservedItemNames.CostOfGoodsSold}"
                ),
                Item(
                    name = "ResearchAndDevelopment",
                    expression = "100"
                ),
                Item(
                    name = "SGA",
                    expression = "100"
                ),
                Item(
                    name = ReservedItemNames.OperatingExpense,
                    expression = "0.0"
                ),
                Item(
                    name = ReservedItemNames.OperatingIncome,
                    expression = "${ReservedItemNames.GrossProfit} - ${ReservedItemNames.OperatingExpense}"
                ),
                Item(
                    name = "LitigationWriteOff",
                    expression = "200"
                ),
                Item(
                    name = ReservedItemNames.NonOperatingExpense,
                    expression = "0.0"
                ),
                Item(
                    name = ReservedItemNames.InterestExpense,
                    expression = "0.0"
                ),
                Item(
                    name = ReservedItemNames.TaxExpense,
                    expression = "0.0"
                ),
                Item(
                    name = ReservedItemNames.NetIncome,
                    expression = "${ReservedItemNames.OperatingIncome} - ${ReservedItemNames.NonOperatingExpense} - ${ReservedItemNames.InterestExpense} - ${ReservedItemNames.TaxExpense}"
                )
            )
        )

        val reformulated = modelBuilder.reformulateModel(updatedModel)

        val cells = modelBuilder.evaluateModel(reformulated)

        val expected = reformulated.incomeStatementItems.size +
                reformulated.balanceSheetItems.size +
                reformulated.otherItems.size

        val actual = cells.size / (model.periods + 1)

        assertEquals(expected, actual)
    }
}
