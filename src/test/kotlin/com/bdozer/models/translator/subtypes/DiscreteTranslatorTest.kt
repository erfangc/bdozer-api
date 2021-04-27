package com.bdozer.models.translator.subtypes

import com.bdozer.models.CellGenerator
import com.bdozer.models.dataclasses.Discrete
import com.bdozer.models.dataclasses.Item
import com.bdozer.models.dataclasses.ItemType
import com.bdozer.models.dataclasses.Model
import com.bdozer.models.translator.FormulaTranslationContext
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class DiscreteTranslatorTest {

    @Test
    fun translateFormula() {
        val model = model()
        val cells = CellGenerator().generateCells(model)
        val ctx = FormulaTranslationContext(
            cells = cells,
            model = model,
        )

        val translator = DiscreteTranslator(ctx = ctx)

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

        val translator = DiscreteTranslator(ctx = ctx)

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

        val translator = DiscreteTranslator(ctx = ctx)

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
                    type = ItemType.Discrete,
                    discrete = Discrete(
                        formulas = mapOf(
                            1 to "100.0",
                            2 to "200.0",
                            3 to "300.0",
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
                    type = ItemType.Discrete,
                    discrete = Discrete(
                        formulas = mapOf(
                            1 to "100.0",
                            2 to "200.0",
                            3 to "300.0",
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
                    type = ItemType.Discrete,
                    discrete = Discrete(
                        formulas = mapOf(
                            1 to "100.0",
                            2 to "200.0",
                            3 to "300.0",
                        )
                    ),
                )
            )
        )
    }
}