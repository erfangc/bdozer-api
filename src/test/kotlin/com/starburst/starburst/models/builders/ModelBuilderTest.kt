package com.starburst.starburst.models.builders

import com.starburst.starburst.models.Item
import com.starburst.starburst.computers.ReservedItemNames
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ModelBuilderTest {

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

        assertEquals(reformulated.incomeStatementItems.size, cells.size / model.periods)
    }
}
