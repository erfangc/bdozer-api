package com.starburst.starburst.edgar.factbase.autofill

import com.starburst.starburst.edgar.factbase.autofill.dataclasses.PercentOfRevenueAutoFill
import com.starburst.starburst.models.dataclasses.Model
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/fact-auto-filler")
class FactAutoFillerController(private val factAutoFiller: FactAutoFiller) {

    @PostMapping("{factId}")
    fun getPercentOfRevenueAutoFills(
        @PathVariable factId: String,
        @RequestBody model: Model,
    ): List<PercentOfRevenueAutoFill> {
        return factAutoFiller.getPercentOfRevenueAutoFills(factId, model)
    }

}