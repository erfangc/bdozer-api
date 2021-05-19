package com.bdozer.api.web.zacks.estimates

import com.bdozer.api.models.dataclasses.ManualProjections
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RequestMapping("api/zacks-estimates")
@RestController
class ZacksEstimatesController(private val zacksEstimatesService: ZacksEstimatesService) {
    @GetMapping("revenue-projections")
    fun revenueProjections(@RequestParam ticker: String): ManualProjections {
        return zacksEstimatesService.revenueProjections(ticker)
    }

    @GetMapping("{ticker}")
    fun getZacksSaleEstimates(@PathVariable ticker: String): List<ZacksSalesEstimates> {
        return zacksEstimatesService.getZacksSaleEstimates(ticker)
    }
}