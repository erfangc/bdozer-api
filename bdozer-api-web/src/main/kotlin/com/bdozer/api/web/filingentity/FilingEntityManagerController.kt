package com.bdozer.api.web.filingentity

import com.bdozer.api.web.filingentity.dataclasses.FilingEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("public/filing-entity-manager")
class FilingEntityManagerController(private val filingEntityManager: FilingEntityManager) {

    @PostMapping
    fun saveFilingEntity(@RequestBody filingEntity: FilingEntity) {
        return filingEntityManager.saveFilingEntity(filingEntity)
    }

    @PostMapping("{cik}/create")
    fun createFilingEntity(@PathVariable cik: String): FilingEntity {
        return filingEntityManager.createFilingEntity(cik)
    }

}
