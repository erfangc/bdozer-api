package com.starburst.starburst.edgar.factbase

import com.starburst.starburst.edgar.dataclasses.Fact
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/fact-base")
@CrossOrigin
class FactBaseController(
    private val factBase: FactBase
) {

    /**
     * Returns a list of facts matching the query
     */
    @GetMapping("{cik}")
    fun queryFacts(
        @PathVariable cik: String,
        @RequestParam nodeName: String,
        @RequestParam(required = false) dimension: String? = null
    ): List<Fact> {
        return factBase.queryFacts(cik, nodeName, dimension)
    }

    @PostMapping("{cik}/{adsh}")
    fun parseAndUploadSingleFiling(
        @PathVariable cik: String,
        @PathVariable adsh: String
    ): ParseUploadSingleFilingResponse {
        return factBase.parseAndUploadSingleFiling(cik, adsh)
    }

}
