package com.starburst.starburst.modelbuilder

import com.starburst.starburst.modelbuilder.common.ModelResult
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/model-builder-factory")
class ModelBuilderFactoryController(private val modelBuilderFactory: ModelBuilderFactory) {
    @GetMapping("{cik}")
    fun createModel(@PathVariable cik: String): ModelResult {
        return modelBuilderFactory.createModel(cik)
    }
}