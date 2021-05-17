package com.bdozer.api.web.stockanalysis

import com.bdozer.api.stockanalysis.kpis.dataclasses.CompanyKPIs
import com.bdozer.api.stockanalysis.kpis.dataclasses.CompanyKPIsService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/company-kpis")
@CrossOrigin
class CompanyKPIsController(private val companyKPIsService: CompanyKPIsService) {
    @GetMapping("{id}")
    fun getCompanyKPIs(@PathVariable  id: String): CompanyKPIs? {
        return companyKPIsService.getCompanyKPIs(id)
    }

    @PostMapping
    fun saveCompanyKPIs(companyKPIs: CompanyKPIs) {
        return companyKPIsService.saveCompanyKPIs(companyKPIs)
    }
}