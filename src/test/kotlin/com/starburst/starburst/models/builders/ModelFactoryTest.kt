package com.starburst.starburst.models.builders

import com.starburst.starburst.Provier.fictitiousSaaSCompany
import com.starburst.starburst.models.Utility
import com.starburst.starburst.models.dataclasses.Item
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ModelFactoryTest {

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
                    name = Utility.Revenue,
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
                    name = Utility.CostOfGoodsSold,
                    expression = "0.0"
                ),
                Item(
                    name = Utility.GrossProfit,
                    expression = "${Utility.Revenue} - ${Utility.CostOfGoodsSold}"
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
                    name = Utility.OperatingExpense,
                    expression = "0.0"
                ),
                Item(
                    name = Utility.OperatingIncome,
                    expression = "${Utility.GrossProfit} - ${Utility.OperatingExpense}"
                ),
                Item(
                    name = "LitigationWriteOff",
                    expression = "200"
                ),
                Item(
                    name = Utility.NonOperatingExpense,
                    expression = "0.0"
                ),
                Item(
                    name = Utility.InterestExpense,
                    expression = "0.0"
                ),
                Item(
                    name = Utility.TaxExpense,
                    expression = "0.0"
                ),
                Item(
                    name = Utility.NetIncome,
                    expression = "${Utility.OperatingIncome} - ${Utility.NonOperatingExpense} - ${Utility.InterestExpense} - ${Utility.TaxExpense}"
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
