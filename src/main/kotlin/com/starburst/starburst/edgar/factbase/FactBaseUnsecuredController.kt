package com.starburst.starburst.edgar.factbase

import com.starburst.starburst.edgar.factbase.dataclasses.AggregatedFact
import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.dataclasses.FilingCalculations
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("public/fact-base")
@CrossOrigin
class FactBaseUnsecuredController(private val factBase: FactBase) {

    @GetMapping("{factId}")
    fun getFact(@PathVariable factId: String): Fact? {
        return factBase.getFact(factId)
    }

    @GetMapping("{cik}/calculations")
    fun calculations(@PathVariable cik: String): FilingCalculations {
        return factBase.calculations(cik.padStart(10, '0'))
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
