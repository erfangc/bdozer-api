package com.starburst.starburst.edgar.filingentity

import com.starburst.starburst.edgar.filingentity.dataclasses.FilingEntity
import com.starburst.starburst.models.dataclasses.Model
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/filing-entity-manager")
class FilingEntityManagerController(private val filingEntityManager: FilingEntityManager) {

    @GetMapping("{cik}/proforma-model")
    fun downloadProformaExcelModel(@PathVariable cik: String): HttpEntity<ByteArray> {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.CONTENT_TYPE, "application/vnd.ms-excel")
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$cik.xlsx")
        return HttpEntity(
            filingEntityManager.downloadProformaExcelModel(cik),
            headers
        )
    }

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

    @PostMapping("{cik}")
    fun bootstrapFilingEntity(@PathVariable cik: String): FilingEntity {
        return filingEntityManager.bootstrapFilingEntity(cik)
    }
}

