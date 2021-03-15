package com.starburst.starburst.zacks

import com.starburst.starburst.models.evaluator.ModelEvaluator
import com.starburst.starburst.models.translator.CellGenerator
import com.starburst.starburst.zacks.modelbuilder.ZacksModelBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.*

@ExperimentalStdlibApi
@RestController
@CrossOrigin
@RequestMapping("api/zacks-model-builder")
class ZacksModelBuilderController(
    private val zacksModelBuilder: ZacksModelBuilder
) {
    @GetMapping("{ticker}")
    fun buildModel(@PathVariable ticker: String): HttpEntity<ByteArray> {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.CONTENT_TYPE, "application/vnd.ms-excel")
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$ticker.xlsx")
        val model = zacksModelBuilder.buildModel(ticker)
        val result = ModelEvaluator().evaluate(model)
        val bytes = CellGenerator.exportToXls(model, result.cells)
        return HttpEntity(bytes, headers)
    }
}