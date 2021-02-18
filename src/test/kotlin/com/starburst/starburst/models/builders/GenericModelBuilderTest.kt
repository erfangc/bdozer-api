package com.starburst.starburst.models.builders

import com.starburst.starburst.models.Item
import com.starburst.starburst.models.ReservedNames
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class GenericModelBuilderTest {

    @Test
    fun reformulateModel() {
        val modelBuilder = GenericModelBuilder()

        val model = modelBuilder.createModel()

        val updatedModel = model.copy(
            items = listOf(
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
                    name = ReservedNames.Revenue,
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
                    name = ReservedNames.CostOfGoodsSold,
                    expression = "0.0"
                ),
                Item(
                    name = ReservedNames.GrossProfit,
                    expression = "${ReservedNames.Revenue} - ${ReservedNames.CostOfGoodsSold}"
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
                    name = ReservedNames.OperatingExpense,
                    expression = "0.0"
                ),
                Item(
                    name = ReservedNames.OperatingIncome,
                    expression = "${ReservedNames.GrossProfit} - ${ReservedNames.OperatingExpense}"
                ),
                Item(
                    name = "LitigationWriteOff",
                    expression = "200"
                ),
                Item(
                    name = ReservedNames.NonOperatingExpense,
                    expression = "0.0"
                ),
                Item(
                    name = ReservedNames.InterestExpense,
                    expression = "0.0"
                ),
                Item(
                    name = ReservedNames.TaxExpense,
                    expression = "0.0"
                ),
                Item(
                    name = ReservedNames.NetIncome,
                    expression = "${ReservedNames.OperatingIncome} - ${ReservedNames.NonOperatingExpense} - ${ReservedNames.InterestExpense} - ${ReservedNames.TaxExpense}"
                )
            )
        )

        val reformulated = modelBuilder.reformulateModel(updatedModel)

        val cells = modelBuilder.evaluateModel(reformulated)

        assertEquals(85, cells.size)
    }
}
