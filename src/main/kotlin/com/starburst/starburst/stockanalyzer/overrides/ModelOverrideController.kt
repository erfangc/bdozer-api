package com.starburst.starburst.stockanalyzer.overrides

import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/model-overrides")
class ModelOverrideController(private val mondelOverrideService: ModelOverrideService) {

    @GetMapping("{cik}")
    fun getOverrides(@PathVariable cik: String): ModelOverride {
        return mondelOverrideService.getOverrides(cik)
    }

    @PostMapping
    fun saveOverrides(@RequestBody body: ModelOverride) {
        return mondelOverrideService.saveOverrides(body)
    }

}