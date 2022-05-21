package com.bdozer.api.web.zacksmodels

import com.bdozer.api.models.dataclasses.BuildZacksModelResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/zacks-model-builder")
class ZacksModelBuilderController(private val zacksModelBuilder: ZacksModelBuilder) {
    @PutMapping("{ticker}")
    fun buildZacksModel(@PathVariable ticker: String): ResponseEntity<BuildZacksModelResponse> {
        val buildZacksModelResponse = zacksModelBuilder.buildZacksModel(ticker)
        return ResponseEntity<BuildZacksModelResponse>(
            buildZacksModelResponse,
            HttpStatus.valueOf(buildZacksModelResponse.status)
        )
    }
}