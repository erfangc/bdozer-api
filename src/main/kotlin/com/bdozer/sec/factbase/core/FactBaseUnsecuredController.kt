package com.bdozer.sec.factbase.core

import com.bdozer.sec.factbase.dataclasses.AggregatedFact
import com.bdozer.sec.factbase.dataclasses.Fact
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