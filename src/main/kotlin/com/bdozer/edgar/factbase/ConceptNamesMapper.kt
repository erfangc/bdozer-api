package com.bdozer.edgar.factbase.ingestor.support

import com.bdozer.edgar.factbase.dataclasses.ConceptNames
import com.bdozer.edgar.factbase.dataclasses.FilingCalculations
import com.bdozer.edgar.factbase.dataclasses.Arc
import java.util.*

internal data class Node(
    val conceptName: String,
    val parent: String? = null
)

class ConceptNamesMapper(private val fc: FilingCalculations) {

    private val revenueCandidates = setOf(
        "Revenues",
        "RevenueFromContractWithCustomerExcludingAssessedTax",
        "RevenueFromContractWithCustomerIncludingAssessedTax",
    )

    private val operatingCostCandidates = setOf(
        "OperatingCostsAndExpenses",
        "CostsAndExpenses",
        "OperatingExpenses",
        "OperatingIncomeLoss",
        "NoninterestExpense",
        "BenefitsLossesAndExpenses",
    )

    private val ebitCandidates = setOf(
        "IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest",
        "IncomeLossFromContinuingOperationsBeforeIncomeTaxesMinorityInterestAndIncomeLossFromEquityMethodInvestments",
    )

    private val epsCandidates = setOf(
        "EarningsPerShareBasicAndDiluted",
        "EarningsPerShareDiluted",
        "EarningsPerShareBasic",
        "IncomeLossFromContinuingOperationsPerBasicShare",
    )

    private val netIncomeCandidates = setOf(
        "NetIncomeLoss",
        "ProfitLoss",
        "NetIncomeLossAvailableToCommonStockholdersBasic",
    )

    fun resolve(): ConceptNames {
        return ConceptNames(
            totalRevenue = totalRevenue(),
            eps = eps(),
            netIncome = netIncome(),
            ebit = ebit(),
            operatingCost = operatingCost(),
        )
    }

    fun totalRevenue():String? {
        return conceptName(
            candidates = revenueCandidates,
            suffix = "RevenuesAbstract",
        )
    }
    fun eps():String? {
        return conceptName(
            candidates = epsCandidates,
            suffix = "EarningsPerShareAbstract",
        )
    }
    fun netIncome():String? {
        return conceptName(
            candidates = netIncomeCandidates,
        )
    }
    fun ebit():String? {
        return conceptName(
            candidates = ebitCandidates,
        )
    }
    fun operatingCost():String? {
        return conceptName(
            candidates = operatingCostCandidates,
        )
    }

    private fun conceptName(candidates: Set<String>, suffix: String = "StatementLineItems"): String? {


        // traverse to find root element
        val topLevelAbstract = topLevelAbstract(fc, suffix)

        val arcs = fc.incomeStatement.associateBy { it.conceptName }.toMutableMap()
        val nodes = fc.incomeStatement.associate { it.conceptName to Node(it.conceptName) }.toMutableMap()

        topLevelAbstract.forEach { arc ->

            //
            // put any of the unprocessed children into the stack
            // stop processing when the stack is empty
            //
            val stack = Stack<String>()
            stack.addAll(dependents(arc))
            while (stack.isNotEmpty()) {
                val childConceptName = stack.pop()
                // tag it's ultimate parent as our parent
                val node = nodes[childConceptName]!!
                nodes[childConceptName] = node.copy(parent = arc.conceptName)

                // if the child have children too then add those for processing
                if (arcs[childConceptName]?.calculations?.isNotEmpty() == true) {
                    stack.addAll(dependents(arcs[childConceptName]!!))
                }
            }
        }

        val result = topLevelAbstract
            .mapNotNull { nodes[it.conceptName] }
            .filter { it.parent == null }

        return result.find { candidates.contains(it.conceptName) }?.conceptName ?:
        fc.incomeStatement.find { candidates.contains(it.conceptName) }?.conceptName

    }

    private fun topLevelAbstract(fc: FilingCalculations, suffix: String): List<Arc> {
        return fc.incomeStatement.filter {
            it.parentHref?.endsWith(suffix) == true &&
                    !it.conceptName.endsWith("Abstract")
        }
    }

    private fun dependents(arc: Arc) =
        arc.calculations.map { calculation ->
            calculation.conceptName
        }
}
