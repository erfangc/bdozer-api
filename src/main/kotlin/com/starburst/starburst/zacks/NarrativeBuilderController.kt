package com.starburst.starburst.zacks

import com.starburst.starburst.zacks.dataclasses.Narrative
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RequestMapping("api/zacks/narrative-builder")
@RestController
class NarrativeBuilderController(
    private val narrativeBuilder: NarrativeBuilder,
) {

    @GetMapping("{ticker}/excel")
    fun exportExcel(@PathVariable ticker: String): HttpEntity<ByteArray> {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.CONTENT_TYPE, "application/vnd.ms-excel")
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$ticker.xlsx")
        val excel = narrativeBuilder.exportExcel(ticker)
        return HttpEntity(excel, headers)
    }

    @GetMapping("{ticker}")
    fun buildNarrative(@PathVariable ticker: String): Narrative {
        return narrativeBuilder.buildNarrative(ticker)
    }
}