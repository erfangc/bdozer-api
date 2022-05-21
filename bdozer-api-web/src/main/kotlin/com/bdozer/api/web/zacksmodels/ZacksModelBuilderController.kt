package com.bdozer.api.web.zacksmodels

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/zacks-model-builder")
class ZacksModelBuilderController(private val zacksModelBuilder: ZacksModelBuilder) {
    @PutMapping("{ticker}")
    fun buildZacksModel(@PathVariable ticker: String): BuildZacksModelResponse {
        return zacksModelBuilder.buildZacksModel(ticker)
    }
}