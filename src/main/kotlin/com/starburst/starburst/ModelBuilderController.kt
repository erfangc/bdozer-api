package com.starburst.starburst

import com.starburst.starburst.models.Cell
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.builders.GenericModelBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ModelBuilderController(private val genericModelBuilder: GenericModelBuilder) {

    @GetMapping("createModel")
    fun createModel(): Model {
        return genericModelBuilder.createModel()
    }

    @PostMapping("evaluateModel")
    fun evaluateModel(@RequestBody model: Model): List<Cell> {
        return genericModelBuilder.evaluateModel(model)
    }

    @PostMapping("reformulateModel")
    fun reformulateModel(@RequestBody model: Model): Model {
        return genericModelBuilder.reformulateModel(model)
    }

}
