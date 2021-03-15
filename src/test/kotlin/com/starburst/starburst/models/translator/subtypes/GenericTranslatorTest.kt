package com.starburst.starburst.models.translator.subtypes

import com.starburst.starburst.models.translator.CellFormulaTranslator
import com.starburst.starburst.models.translator.CellGenerator
import com.starburst.starburst.models.translator.FormulaTranslationContext
import com.starburst.starburst.models.dataclasses.ItemType
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.models.dataclasses.FixedCost
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
            .populateCellsWithFormulas(model, CellGenerator().generateCells(model = model))

        val ctx = FormulaTranslationContext(
            model = model,
            cells = cells
        )

        val result = GenericTranslator(ctx)
            .translateFormula(cells.find { it.name == "Random_Period2" }!!)

        assertEquals("(Aircraft_Parts_Period2+Aircraft_Parts_Period2)/2", result.formula)

    }
}
