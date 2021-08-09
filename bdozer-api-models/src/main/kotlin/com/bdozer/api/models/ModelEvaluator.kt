package com.bdozer.api.models

import com.bdozer.api.models.dataclasses.EvaluateModelResult
import com.bdozer.api.models.dataclasses.Utility.PresentValuePerShare
import com.bdozer.api.models.dataclasses.Model

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
        return EvaluateModelResult(
            model = overriddenModel,
            cells = evaluatedCells
        )
    }
}