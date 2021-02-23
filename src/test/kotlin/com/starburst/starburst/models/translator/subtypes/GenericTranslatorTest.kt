package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.models.translator.CellFormulaTranslator
import com.starburst.starburst.models.translator.ModelToCellTranslator
import com.starburst.starburst.computers.ResolverContext
import com.starburst.starburst.models.ItemType
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.translator.subtypes.dataclasses.FixedCost
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class GenericTranslatorTest {

    @Test
    fun resolveExpression() {
        val model = Model(
            incomeStatementItems = listOf(
                Item(
                    name = "Aircraft_Parts",
                    fixedCost = FixedCost(1000.0),
                    type = ItemType.FixedCost
                ),
                Item(
                    expression = "(Aircraft_Parts + Aircraft_Parts) / 2",
                    name = "Random"
                )
            ),
            periods = 2
        )

        val cells = CellFormulaTranslator()
            .populateCellsWithFormulas(model, ModelToCellTranslator().generateCells(model = model))

        val ctx = ResolverContext(
            model = model,
            cells = cells
        )

        val result = GenericTranslator(ctx)
            .translateFormula(cells.find { it.name == "Random_Period2" }!!)

        assertEquals("(Aircraft_Parts_Period2+Aircraft_Parts_Period2)/2", result.formula)

    }
}
