package com.starburst.starburst.models.builders

import com.starburst.starburst.Provier.fictitiousSaaSCompany
import com.starburst.starburst.computers.ReservedItemNames
import com.starburst.starburst.models.Item
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

        val cells = modelBuilder.evaluateModel(reformulated).cells

        val expected = reformulated.incomeStatementItems.size +
                reformulated.balanceSheetItems.size +
                reformulated.otherItems.size

        val actual = cells.size / (model.periods + 1)

        assertEquals(expected, actual)
    }

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

        val cells = modelBuilder.evaluateModel(reformulated).cells

        val expected = reformulated.incomeStatementItems.size +
                reformulated.balanceSheetItems.size +
                reformulated.otherItems.size

        val actual = cells.size / (model.periods + 1)

        assertEquals(expected, actual)
    }
}
