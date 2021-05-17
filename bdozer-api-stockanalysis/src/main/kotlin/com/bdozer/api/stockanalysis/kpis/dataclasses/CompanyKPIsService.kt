package com.bdozer.api.stockanalysis.kpis.dataclasses

import com.bdozer.api.models.CellEvaluator
import com.bdozer.api.models.CellGenerator
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
     * Evaluate KPIs from logical relations declared in items
     * to a set of cells
     */
    fun evaluate(companyKPIs: CompanyKPIs):CompanyKPIs {
        val items = companyKPIs.items
        val projectionPeriods = companyKPIs.projectionPeriods
        val cellGenerator = CellGenerator()
        val unevaluatedCells = cellGenerator.generateCells(items, projectionPeriods)
        val cellEvaluator = CellEvaluator()
        val cells = cellEvaluator.evaluate(unevaluatedCells)
        return companyKPIs.copy(cells = cells)
    }
}