package com.starburst.starburst.modelbuilder

import com.starburst.starburst.models.dataclasses.Model
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/model-builder-factory")
class ModelBuilderFactoryController(private val modelBuilderFactory: ModelBuilderFactory) {

    @GetMapping("{cik}")
    fun model(@PathVariable cik: String): Model {
        return modelBuilderFactory.createModel(cik)
    }
}