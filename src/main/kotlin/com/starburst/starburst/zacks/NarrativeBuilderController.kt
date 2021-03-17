package com.starburst.starburst.zacks

import com.starburst.starburst.zacks.dataclasses.Narrative
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RequestMapping("api/zacks/narrative-builder")
@RestController
class NarrativeBuilderController(
    private val narrativeBuilder: NarrativeBuilder,
) {
    @GetMapping
    fun buildNarrative(@RequestParam ticker: String): Narrative {
        return narrativeBuilder.buildNarrative(ticker)
    }
}