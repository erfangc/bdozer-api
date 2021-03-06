package com.starburst.starburst.edgar.factbase.filingentity

import com.starburst.starburst.edgar.factbase.filingentity.dataclasses.FilingEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/filing-entity-manager")
class FilingEntityManagerController(private val filingEntityManager: FilingEntityManager) {
    @GetMapping("{cik}")
    fun getFilingEntity(@PathVariable cik: String): FilingEntity {
        return filingEntityManager.getFilingEntity(cik)
    }
}

