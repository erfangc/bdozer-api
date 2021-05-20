package com.bdozer.api.stockanalysis.kpis.dataclasses

import bdozer.api.common.extensions.DoubleExtensions.orZero
import com.bdozer.api.models.CellEvaluator
import com.bdozer.api.models.CellGenerator
import com.bdozer.api.models.dataclasses.HistoricalValue
import com.bdozer.api.models.dataclasses.Item
import com.bdozer.api.models.dataclasses.ItemType
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import java.util.*

class CompanyKPIsService(mongoDatabase: MongoDatabase) {

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
    fun evaluateCompanyKPIs(companyKPIs: CompanyKPIs): CompanyKPIs {
        val projectionPeriods = companyKPIs.projectionPeriods
        val items = processItems(companyKPIs)
        val cellGenerator = CellGenerator()
        val unevaluatedCells = cellGenerator.generateCells(items, projectionPeriods)
        val cellEvaluator = CellEvaluator()
        val cells = cellEvaluator.evaluate(unevaluatedCells)
        return companyKPIs.copy(cells = cells)
    }

    private fun processItems(
        companyKPIs: CompanyKPIs
    ): List<Item> {
        val items = companyKPIs.items

        /**
         * Find the parents of a given item
         */
        fun parentsOf(item: Item): List<Item> {
            return items.filter { candidate ->
                when (candidate.type) {
                    ItemType.SumOfOtherItems -> {
                        candidate
                            .sumOfOtherItems
                            ?.components
                            ?.any { it.itemName == item.name } ?: false
                    }
                    ItemType.ProductOfOtherItems -> {
                        candidate
                            .productOfOtherItems
                            ?.components
                            ?.any { it.itemName == item.name } ?: false
                    }
                    else -> {
                        false
                    }
                }
            }
        }

        /*
        make sure the Period 0 numbers are coherent, run an algorithm to populate all period 1 historical value
         */
        val itemsLookup = companyKPIs.items.associateBy { it.name }.toMutableMap()

        val driverItems =
            companyKPIs.items.filter { it.type != ItemType.ProductOfOtherItems && it.type != ItemType.SumOfOtherItems }
        val queue = ArrayDeque<Item>()
        val visited = hashSetOf<String>()
        queue.addAll(driverItems)

        fun processSelf(item: Item): Item {
            return when (item.type) {
                ItemType.SumOfOtherItems -> {
                    val sum = item.sumOfOtherItems?.components?.mapNotNull { component ->
                        val itemName = component.itemName
                        itemsLookup[itemName]?.historicalValue?.value
                    }?.sum().orZero()
                    item.copy(historicalValue = HistoricalValue(value = sum))
                }
                ItemType.ProductOfOtherItems -> {
                    val prod = item.productOfOtherItems?.components?.mapNotNull { component ->
                        val itemName = component.itemName
                        itemsLookup[itemName]?.historicalValue?.value
                    }?.reduce { acc, curr -> acc * curr }.orZero()
                    item.copy(historicalValue = HistoricalValue(value = prod))
                }
                else -> {
                    item
                }
            }
        }

        while (queue.isNotEmpty()) {
            val item = queue.poll()
            visited.add(item.name)
            // update self historical value
            itemsLookup[item.name] = processSelf(item)
            val parents = parentsOf(item)
                .filter { !visited.contains(it.name) && !queue.any { queueItem -> queueItem.name == it.name } }
            queue.addAll(parents)
        }

        return items.mapNotNull { item -> itemsLookup[item.name] }
    }
}