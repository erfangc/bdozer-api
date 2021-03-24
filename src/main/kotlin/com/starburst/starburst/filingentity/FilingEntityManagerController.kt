package com.starburst.starburst.filingentity

import com.starburst.starburst.filingentity.dataclasses.FilingEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/filing-entity-manager")
class FilingEntityManagerController(private val filingEntityManager: FilingEntityManager) {

    @GetMapping("{cik}")
    fun getFilingEntity(@PathVariable cik: String): FilingEntity {
        return filingEntityManager.getOrBootstrapFilingEntity(cik)
    }

    @PostMapping
    fun saveFilingEntity(@RequestBody filingEntity: FilingEntity) {
        return filingEntityManager.saveFilingEntity(filingEntity)
    }

    @PostMapping("{cik}")
    fun bootstrapFilingEntity(@PathVariable cik: String): FilingEntity {
        return filingEntityManager.bootstrapFilingEntity(cik)
    }

}

