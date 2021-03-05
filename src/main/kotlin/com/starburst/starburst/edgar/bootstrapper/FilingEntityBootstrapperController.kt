package com.starburst.starburst.edgar.bootstrapper

import com.starburst.starburst.models.Model
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

    @PostMapping("build-model-with-latest-10-k")
    fun buildModelWithLatest10K(@RequestParam cik: String): Model {
        return filingEntityBootstrapper.buildModelWithLatest10K(cik)
    }
}