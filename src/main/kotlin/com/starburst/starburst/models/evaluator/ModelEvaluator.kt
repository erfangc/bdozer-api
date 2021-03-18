package com.starburst.starburst.models.evaluator

import com.starburst.starburst.models.Utility.PresentValuePerShare
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.models.translator.CellGenerator
import com.starburst.starburst.spreadsheet.evaluation.CellEvaluator
import org.springframework.stereotype.Service

/**
 * [ModelEvaluator] evaluates a model and return the cells and target price
 */
@Service
class ModelEvaluator {
    /*
    Run the model
     */
    fun evaluate(model: Model): EvaluateModelResult {
        val cells = CellGenerator().generateCells(model)
        val evaluatedCells = CellEvaluator().evaluate(cells)
        val targetPrice = evaluatedCells
            .filter { cell -> cell.item.name == PresentValuePerShare }
            .sumByDouble { it.value ?: 0.0 }
        return EvaluateModelResult(
            model = model,
            cells = evaluatedCells,
            targetPrice = targetPrice
        )
    }
}