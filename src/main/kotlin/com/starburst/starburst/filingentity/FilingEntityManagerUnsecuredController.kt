package com.starburst.starburst.filingentity

import com.starburst.starburst.filingentity.dataclasses.FilingEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("public/filing-entity-manager")
class FilingEntityManagerUnsecuredController(private val filingEntityManager: FilingEntityManager) {

    @GetMapping("{cik}")
    fun getFilingEntity(@PathVariable cik: String): FilingEntity? {
        return filingEntityManager.getFilingEntity(cik)
    }

}

