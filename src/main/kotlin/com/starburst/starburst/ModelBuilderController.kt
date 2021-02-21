package com.starburst.starburst

import com.starburst.starburst.spreadsheet.Cell
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.builders.ModelBuilder
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
class ModelBuilderController(private val modelBuilder: ModelBuilder) {

    @GetMapping("createModel")
    fun createModel(): Model {
        return modelBuilder.createModel()
    }

    @PostMapping("evaluateModel")
    fun evaluateModel(@RequestBody model: Model): List<Cell> {
        return modelBuilder.evaluateModel(model)
    }

    @PostMapping("reformulateModel")
    fun reformulateModel(@RequestBody model: Model): Model {
        return modelBuilder.reformulateModel(model)
    }

}
