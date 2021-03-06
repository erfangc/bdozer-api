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

    @GetMapping("{cik}/model-with-latest-10-k")
    fun buildModelWithLatest10K(@PathVariable cik: String): Model {
        return filingEntityManager.modelWithLatest10K(cik)
    }
}

