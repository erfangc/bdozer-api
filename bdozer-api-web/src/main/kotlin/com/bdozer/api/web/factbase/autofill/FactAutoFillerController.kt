package com.bdozer.api.web.factbase.autofill

import com.bdozer.api.web.models.dataclasses.Model
import com.bdozer.api.web.factbase.autofill.dataclasses.FixedCostAutoFill
import com.bdozer.api.web.factbase.autofill.dataclasses.PercentOfAnotherItemAutoFill
import com.bdozer.api.web.factbase.autofill.dataclasses.PercentOfRevenueAutoFill
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/fact-auto-filler")
class FactAutoFillerController(private val factAutoFiller: FactAutoFiller) {

    @PostMapping("{itemName}/percent-of-revenue")
    fun getPercentOfRevenueAutoFills(
        @PathVariable itemName: String,
        @RequestBody model: Model,
    ): List<PercentOfRevenueAutoFill> {
        return factAutoFiller.getPercentOfRevenueAutoFills(itemName, model)
    }

    @PostMapping("{itemName}/percent-of-another-item")
    fun getPercentOfItemsAutoFills(
        @PathVariable itemName: String,
        @RequestParam dependentItemName: String,
        @RequestBody model: Model,
    ): List<PercentOfAnotherItemAutoFill> {
        return factAutoFiller.getPercentOfAnotherItemAutoFills(itemName, dependentItemName, model)
    }

    @PostMapping("{factId}/fixed-cost")
    fun getFixedCostAutoFills(
        @PathVariable factId: String,
        @RequestBody model: Model,
    ): List<FixedCostAutoFill> {
        return factAutoFiller.getFixedCostAutoFills(factId, model)
    }

}