package com.starburst.starburst

import com.starburst.starburst.models.Cell
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.builders.ModelBuilderService
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
class ModelBuilderController(private val modelBuilderService: ModelBuilderService) {

    @GetMapping("createModel")
    fun createModel(): Model {
        return modelBuilderService.createModel()
    }

    @PostMapping("evaluateModel")
    fun evaluateModel(@RequestBody model: Model): List<Cell> {
        return modelBuilderService.evaluateModel(model)
    }

    @PostMapping("reformulateModel")
    fun reformulateModel(@RequestBody model: Model): Model {
        return modelBuilderService.reformulateModel(model)
    }

}
