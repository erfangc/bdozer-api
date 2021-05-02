package com.bdozer.api.web.factbase

import com.bdozer.api.factbase.core.dataclasses.AggregatedFact
import com.bdozer.api.factbase.core.dataclasses.Fact
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("public/fact-base")
@CrossOrigin
class FactBaseController(private val factBase: FactBase) {

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
