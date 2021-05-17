package com.bdozer.api.stockanalysis.kpis.dataclasses

import com.bdozer.api.models.CellEvaluator
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save

class CompanyKPIsService(
    private val mongoDatabase: MongoDatabase
) {

    val col = mongoDatabase.getCollection<CompanyKPIs>()

    fun getCompanyKPIs(id: String): CompanyKPIs? {
        return col.findOneById(id)
    }

    fun saveCompanyKPIs(companyKPIs: CompanyKPIs) {
        col.save(companyKPIs)
    }

    /**
     * TODO figure out how to predict KPIMetadata metrics based on CAGR
     */
    fun evaluate(companyKPIs: CompanyKPIs) {
        val cells = CellEvaluator()
    }
}