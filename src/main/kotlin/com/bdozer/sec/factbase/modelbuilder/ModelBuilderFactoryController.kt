package com.bdozer.sec.factbase.modelbuilder

import com.bdozer.models.dataclasses.Model
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("api/sec/model-builder-factory")
class ModelBuilderFactoryController(
    private val modelBuilderFactory: ModelBuilderFactory
) {
    @GetMapping
    fun bestEffortModel(
        @RequestParam cik: String,
        @RequestParam adsh: String
    ): Model {
        return modelBuilderFactory.bestEffortModel(cik, adsh)
    }
}