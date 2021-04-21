package com.bdozer.models

import com.bdozer.models.Utility.PresentValuePerShare
import com.bdozer.models.dataclasses.Item
import com.bdozer.models.dataclasses.Model
import com.bdozer.spreadsheet.evaluation.CellEvaluator
import org.springframework.stereotype.Service

@Service
class ModelEvaluator {

    /*
    Run the model
     */
    fun evaluate(model: Model): EvaluateModelResult {
        /*
        overlay items form the override to model
         */
        val overriddenModel = model.override()
        val cells = CellGenerator().generateCells(overriddenModel)
        val evaluatedCells = CellEvaluator().evaluate(cells)
        val targetPrice = evaluatedCells
            .filter { cell -> cell.item.name == PresentValuePerShare }
            .sumByDouble { it.value ?: 0.0 }
        return EvaluateModelResult(
            model = overriddenModel,
            cells = evaluatedCells,
            targetPrice = targetPrice
        )
    }
}