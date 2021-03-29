package com.starburst.starburst.filingentity

import com.starburst.starburst.filingentity.dataclasses.FilingEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("public/filing-entity-manager1")
class PublicFilingEntityManagerController1(private val filingEntityManager: FilingEntityManager) {

    @GetMapping("{cik}")
    fun getFilingEntity(@PathVariable cik: String): FilingEntity {
        return filingEntityManager.getOrBootstrapFilingEntity(cik)
    }

}

