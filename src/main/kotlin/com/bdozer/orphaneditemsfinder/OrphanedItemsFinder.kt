package com.bdozer.orphaneditemsfinder

import com.bdozer.models.dataclasses.Item
import com.bdozer.stockanalysis.dataclasses.StockAnalysis2
import java.util.*

/**
 * This class traverses the income statement of an model
 * in search for orphaned items. These are items that are not referenced by
 * any other items and thus likely indicate a data issue
 */
class OrphanedItemsFinder {

    fun findOrphanedItems(stockAnalysis: StockAnalysis2): List<Item> {

        val model = stockAnalysis.model.override()
        val incomeStatement = model.incomeStatementItems

        val revenueItemName = model.totalRevenueConceptName ?: return emptyList()
        val revenueItem = incomeStatement.find { it.name == revenueItemName } ?: return emptyList()
        val netIncomeItemName = model.netIncomeConceptName ?: return emptyList()
        val netIncomeItem = incomeStatement.find { it.name == netIncomeItemName } ?: return emptyList()

        fun children(item: Item?): List<Item> {
            return if (item?.sumOfOtherItems == null) {
                emptyList()
            } else {
                item.sumOfOtherItems.components.mapNotNull { component ->
                    incomeStatement.find { it.name == component.itemName }
                }
            }
        }

        val parents = incomeStatement
            .flatMap { parent ->
                val children = children(parent)
                children.map { child ->
                    child to parent
                }
            }
            .groupBy { it.first }
            .mapValues { (_, values) -> values.map { it.second } }

        val visited = hashSetOf(revenueItem)
        /*
        Traverse revenue forest
         */
        val itemsToVisit = Stack<Item>()
        itemsToVisit.addAll(parents[revenueItem] ?: emptyList())
        while (itemsToVisit.isNotEmpty()) {
            val item = itemsToVisit.pop()
            visited.add(item)
            // links = children + parents of this node
            val parentItems = (parents[item] ?: emptyList()) + children(item)
            itemsToVisit.addAll(parentItems.filter { !visited.contains(it) })
        }

        if (visited.contains(netIncomeItem)) {
            return emptyList()
        } else {
            /*
            Traverse net income forest
             */
            itemsToVisit.addAll(children(netIncomeItem))
            while (itemsToVisit.isNotEmpty()) {
                val item = itemsToVisit.pop()
                visited.add(item)
                val elements = children(item)
                itemsToVisit.addAll(elements.filter { !visited.contains(it) })
            }

            val revenueIndex = incomeStatement.indexOfFirst { it.name == revenueItemName }
            val netIncomeIndex = incomeStatement.indexOfFirst { it.name == netIncomeItemName }

            /*
            every thing that is unvisited (except those that cuts off after NetIncome)
            is an orphan
             */
            return if (netIncomeIndex > revenueIndex) {
                val allItems = incomeStatement.subList(revenueIndex, netIncomeIndex).toSet()
                allItems.minus(visited).toList()
            } else {
                emptyList()
            }
        }

    }

}