package com.bdozer.api.web.filingentity

import com.bdozer.api.filing.entity.FilingEntityManager
import com.bdozer.api.filing.entity.dataclasses.FilingEntity
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

