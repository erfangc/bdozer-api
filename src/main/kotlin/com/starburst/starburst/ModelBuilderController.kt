package com.starburst.starburst

import com.starburst.starburst.models.Cell
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.builders.ModelService
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
class ModelBuilderController(private val modelService: ModelService) {

    @GetMapping("createModel")
    fun createModel(): Model {
        return modelService.createModel()
    }

    @PostMapping("evaluateModel")
    fun evaluateModel(@RequestBody model: Model): List<Cell> {
        return modelService.evaluateModel(model)
    }

    @PostMapping("reformulateModel")
    fun reformulateModel(@RequestBody model: Model): Model {
        return modelService.reformulateModel(model)
    }

}
