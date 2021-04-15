package com.bdozer.edgar.factbase.autofill

import com.bdozer.edgar.factbase.autofill.dataclasses.FixedCostAutoFill
import com.bdozer.edgar.factbase.autofill.dataclasses.PercentOfRevenueAutoFill
import com.bdozer.models.dataclasses.Model
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/fact-auto-filler")
class FactAutoFillerController(private val factAutoFiller: FactAutoFiller) {

    @PostMapping("{factId}/percent-of-revenue")
    fun getPercentOfRevenueAutoFills(
        @PathVariable factId: String,
        @RequestBody model: Model,
    ): List<PercentOfRevenueAutoFill> {
        return factAutoFiller.getPercentOfRevenueAutoFills(factId, model)
    }

    @PostMapping("{factId}/fixed-cost")
    fun getFixedCostAutoFills(
        @PathVariable factId: String,
        @RequestBody model: Model,
    ): List<FixedCostAutoFill> {
        return factAutoFiller.getFixedCostAutoFills(factId, model)
    }

}