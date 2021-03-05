package com.starburst.starburst.edgar.factbase

import com.starburst.starburst.edgar.dataclasses.Fact
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/fact-base")
@CrossOrigin
class FactBaseController(
    private val factBase: FactBase
) {

    @PostMapping("{cik}/{adsh}")
    fun parseAndUploadSingleFiling(
        @PathVariable cik: String,
        @PathVariable adsh: String
    ): ParseUploadSingleFilingResponse {
        return factBase.parseAndUploadSingleFiling(cik, adsh)
    }

}
