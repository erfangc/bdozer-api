package com.bdozer.models.translator.subtypes

import com.bdozer.models.CellGenerator
import com.bdozer.models.dataclasses.*
import com.bdozer.models.translator.FormulaTranslationContext
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ManualProjectionsTranslatorTest {

    @Test
    fun translateFormula() {
        val model = model()
        val cells = CellGenerator().generateCells(model)
        val ctx = FormulaTranslationContext(
            cells = cells,
            model = model,
        )

        val translator = ManualProjectionsTranslator(ctx = ctx)

        val relevantCells = cells.filter { cell -> cell.item.name == "Test" }
        assertEquals(
            "401.625",
            translator.translateFormula(cell = relevantCells[model.periods]).formula
        )
        assertEquals(
            "382.5",
            translator.translateFormula(cell = relevantCells[model.periods - 1]).formula
        )
        assertEquals(
            "300.0",
            translator.translateFormula(cell = relevantCells[model.periods - 2]).formula
        )
        assertEquals(
            "200.0",
            translator.translateFormula(cell = relevantCells[model.periods - 3]).formula
        )
        assertEquals(
            "100.0",
            translator.translateFormula(cell = relevantCells[model.periods - 4]).formula
        )
    }

    @Test
    fun translateFormula2() {
        val model = model2()
        val cells = CellGenerator().generateCells(model)
        val ctx = FormulaTranslationContext(
            cells = cells,
            model = model,
        )

        val translator = ManualProjectionsTranslator(ctx = ctx)

        val relevantCells = cells.filter { cell -> cell.item.name == "Test" }
        assertEquals(
            "300.0",
            translator.translateFormula(cell = relevantCells[model.periods]).formula
        )
        assertEquals(
            "200.0",
            translator.translateFormula(cell = relevantCells[model.periods - 1]).formula
        )
        assertEquals(
            "100.0",
            translator.translateFormula(cell = relevantCells[model.periods - 2]).formula
        )
    }

    @Test
    fun translateFormula3() {
        val model = model3()
        val cells = CellGenerator().generateCells(model)
        val ctx = FormulaTranslationContext(
            cells = cells,
            model = model,
        )

        val translator = ManualProjectionsTranslator(ctx = ctx)

        val relevantCells = cells.filter { cell -> cell.item.name == "Test" }
        assertEquals(
            "200.0",
            translator.translateFormula(cell = relevantCells[model.periods]).formula
        )
        assertEquals(
            "100.0",
            translator.translateFormula(cell = relevantCells[model.periods - 1]).formula
        )
    }

    private fun model(): Model {
        return Model(
            periods = 5,
            terminalGrowthRate = 0.05,
            incomeStatementItems = listOf(
                Item(
                    name = "Test",
                    type = ItemType.ManualProjections,
                    manualProjections = ManualProjections(
                        manualProjections = listOf(
                            ManualProjection(period = 1, value = 100.0),
                            ManualProjection(period = 2, value = 200.0),
                            ManualProjection(period = 3, value = 300.0),
                        )
                    ),
                )
            )
        )
    }

    private fun model3(): Model {
        return Model(
            periods = 2,
            terminalGrowthRate = 0.05,
            incomeStatementItems = listOf(
                Item(
                    name = "Test",
                    type = ItemType.ManualProjections,
                    manualProjections = ManualProjections(
                        manualProjections = listOf(
                            ManualProjection(period = 1, value = 100.0),
                            ManualProjection(period = 2, value = 200.0),
                            ManualProjection(period = 3, value = 300.0),
                        )
                    ),
                )
            )
        )
    }

    private fun model2(): Model {
        return Model(
            periods = 3,
            terminalGrowthRate = 0.05,
            incomeStatementItems = listOf(
                Item(
                    name = "Test",
                    type = ItemType.ManualProjections,
                    manualProjections = ManualProjections(
                        manualProjections = listOf(
                            ManualProjection(period = 1, value = 100.0),
                            ManualProjection(period = 2, value = 200.0),
                            ManualProjection(period = 3, value = 300.0),
                        )
                    ),
                )
            )
        )
    }
}