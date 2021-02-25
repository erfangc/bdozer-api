package com.starburst.starburst.models.builders

import com.starburst.starburst.models.Model
import com.starburst.starburst.models.ModelEvaluationOutput
import com.starburst.starburst.models.builders.ModelBuilder
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
