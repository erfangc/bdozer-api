package com.bdozer.zacks.se

import com.bdozer.models.dataclasses.ManualProjections
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