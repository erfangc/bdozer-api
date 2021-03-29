package com.starburst.starburst.filingentity

import com.starburst.starburst.filingentity.dataclasses.FilingEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/filing-entity-manager")
class FilingEntityManagerController(private val filingEntityManager: FilingEntityManager) {

    @PostMapping
    fun saveFilingEntity(@RequestBody filingEntity: FilingEntity) {
        return filingEntityManager.saveFilingEntity(filingEntity)
    }

    @PostMapping("{cik}")
    fun bootstrapFilingEntity(@PathVariable cik: String): FilingEntity {
        return filingEntityManager.bootstrapFilingEntity(cik)
    }

}

