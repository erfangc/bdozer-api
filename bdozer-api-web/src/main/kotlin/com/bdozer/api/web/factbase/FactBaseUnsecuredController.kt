package com.bdozer.api.web.factbase

import com.bdozer.api.common.dataclasses.sec.AggregatedFact
import com.bdozer.api.common.dataclasses.sec.Fact
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("public/fact-base")
@CrossOrigin
class FactBaseUnsecuredController(private val factBase: FactBase) {

    @GetMapping("{factId}")
    fun getFact(@PathVariable factId: String): Fact? {
        return factBase.getFact(factId)
    }

    @GetMapping("time-series")
    fun getAnnualTimeSeries(@RequestParam factIds: List<String>): List<AggregatedFact> {
        return factBase.getAnnualTimeSeries(factIds)
    }

    @GetMapping("{factId}/time-series")
    fun getAnnualTimeSeries(@PathVariable factId: String): List<Fact> {
        return factBase.getAnnualTimeSeries(factId)
    }

}
