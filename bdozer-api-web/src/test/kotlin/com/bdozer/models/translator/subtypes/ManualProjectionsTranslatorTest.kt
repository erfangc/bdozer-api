package com.bdozer.models.translator.subtypes

import bdozer.api.common.model.*
import com.bdozer.api.web.models.CellGenerator
import com.bdozer.api.web.models.translator.FormulaTranslationContext
import com.bdozer.api.web.models.translator.subtypes.ManualProjectionsTranslator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
            incomeStatementItems = listOf(item())
        )
    }

    private fun model3(): Model {
        return Model(
            periods = 2,
            terminalGrowthRate = 0.05,
            incomeStatementItems = listOf(item())
        )
    }

    private fun model2(): Model {
        return Model(
            periods = 3,
            terminalGrowthRate = 0.05,
            incomeStatementItems = listOf(item())
        )
    }

    private fun item() = Item(
        name = "Test",
        type = ItemType.ManualProjections,
        /*
        for these tests, our fy0 is set to 2020,
        this period 1 = 2021
        this period 2 = 2022
        and so on
         */
        historicalValue = HistoricalValue(documentFiscalYearFocus = 2020),
        manualProjections = ManualProjections(
            manualProjections = listOf(
                ManualProjection(fiscalYear = 2021, value = 100.0),
                ManualProjection(fiscalYear = 2022, value = 200.0),
                ManualProjection(fiscalYear = 2023, value = 300.0),
            )
        ),
    )
}