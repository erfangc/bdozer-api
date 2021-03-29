package com.starburst.starburst.edgar.factbase

import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.dataclasses.FactTimeSeries
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("public/fact-base")
@CrossOrigin
class FactBaseUnsecuredController(
    private val factBase: FactBase,
) {

    @GetMapping("{cik}/facts")
    fun facts(@PathVariable cik: String): List<Fact> {
        return factBase.getFacts(cik)
    }

    @GetMapping("{factId}/time-series")
    fun getFactTimeSeries(@PathVariable factId: String): FactTimeSeries {
        return factBase.getFactTimeSeries(factId)
    }

    @GetMapping("{cik}/calculations")
    fun calculations(@PathVariable cik: String) = factBase.calculations(cik.padStart(10, '0'))

}
