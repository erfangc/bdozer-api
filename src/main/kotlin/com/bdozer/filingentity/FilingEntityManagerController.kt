package com.bdozer.filingentity

import com.bdozer.filingentity.dataclasses.FilingEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("public/filing-entity-manager")
class FilingEntityManagerController(
    private val filingEntityManager: FilingEntityManager,
    private val filingEntityBootstrapper: FilingEntityBootstrapper,
) {

    @PostMapping
    fun saveFilingEntity(@RequestBody filingEntity: FilingEntity) {
        return filingEntityManager.saveFilingEntity(filingEntity)
    }

    @PostMapping("{cik}/create")
    fun createFilingEntity(@PathVariable cik: String): FilingEntity {
        return filingEntityBootstrapper.createFilingEntity(cik)
    }

    @PostMapping("{cik}")
    fun bootstrapFilingEntity(@PathVariable cik: String): FilingEntity {
        return filingEntityBootstrapper.bootstrapFilingEntity(cik)
    }

    @PostMapping("{cik}/sync")
    fun bootstrapFilingEntitySync(@PathVariable cik: String): FilingEntity {
        return filingEntityBootstrapper.bootstrapFilingEntitySync(cik)
    }

}
