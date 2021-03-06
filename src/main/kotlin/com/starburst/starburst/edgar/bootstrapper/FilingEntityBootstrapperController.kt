package com.starburst.starburst.edgar.bootstrapper

import org.springframework.web.bind.annotation.*
import java.util.concurrent.Executors

@RestController
@CrossOrigin
@RequestMapping("api/filing-entity-bootstrapper")
class FilingEntityBootstrapperController(
    private val filingEntityBootstrapper: FilingEntityBootstrapper
) {

    private val executor = Executors.newCachedThreadPool()

    @PostMapping
    fun bootstrapFilingEntity(@RequestParam cik: String) {
        executor.execute {
            filingEntityBootstrapper.bootstrapFilingEntity(cik)
        }
    }

}