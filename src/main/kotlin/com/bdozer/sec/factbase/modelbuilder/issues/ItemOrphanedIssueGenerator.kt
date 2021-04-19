package com.bdozer.sec.factbase.modelbuilder.issues

import com.bdozer.models.dataclasses.Item
import com.bdozer.stockanalysis.dataclasses.StockAnalysis2
import java.util.*

class ItemOrphanedIssueGenerator {

    fun generateIssues(stockAnalysis: StockAnalysis2): List<Issue> {

        val model = stockAnalysis.model
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
            val allItems = incomeStatement.subList(revenueIndex, netIncomeIndex).toSet()
            val orphans = allItems.minus(visited)
            return orphans.map { item ->
                Issue(
                    _id = "${stockAnalysis._id}${IssueType.OrphanItem}${item.name}",
                    stockAnalysisId = stockAnalysis._id,
                    itemName = item.name,
                    issueType = IssueType.OrphanItem,
                    message = "${item.name} is an orphan (not referenced by any calculations)",
                )
            }
        }

    }

}