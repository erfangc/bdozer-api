package com.bdozer.api.web.zacks.se

import bdozer.api.common.model.ManualProjections
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RequestMapping("api/zacks-estimates")
@RestController
class ZacksEstimatesController(private val zacksEstimatesService: ZacksEstimatesService) {
    @GetMapping("revenue-projections")
    fun revenueProjections(@RequestParam ticker: String): ManualProjections {
        return zacksEstimatesService.revenueProjections(ticker)
    }
}