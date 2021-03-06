package com.starburst.starburst.edgar.factbase.filingentity

import com.starburst.starburst.edgar.factbase.filingentity.dataclasses.FilingEntity
import com.starburst.starburst.models.Model
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/filing-entity-manager")
class FilingEntityManagerController(private val filingEntityManager: FilingEntityManager) {
    @GetMapping("{cik}")
    fun getFilingEntity(@PathVariable cik: String): FilingEntity {
        return filingEntityManager.getFilingEntity(cik)
    }

    @GetMapping("{cik}/view-latest-10k-model")
    fun viewLatest10kModel(@PathVariable cik: String): Model {
        return filingEntityManager.viewLatest10kModel(cik)
    }

    @PostMapping("{cik}/rerun-model")
    fun rerunModel(@PathVariable cik: String): Model {
        return filingEntityManager.rerunModel(cik)
    }
}

