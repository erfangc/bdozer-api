package com.starburst.starburst.models.builders

import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.models.dataclasses.ModelEvaluationOutput
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RequestMapping("api/model-builder")
@RestController
class ModelBuilderController(private val modelBuilder: ModelBuilder) {

    @GetMapping("createModel")
    fun createModel(): Model {
        return modelBuilder.createModel()
    }

    @PostMapping("evaluateModel")
    fun evaluateModel(@RequestBody model: Model): ModelEvaluationOutput {
        return modelBuilder.evaluateModel(model)
    }

    @PostMapping("reformulateModel")
    fun reformulateModel(@RequestBody model: Model): Model {
        return modelBuilder.reformulateModel(model)
    }

}
