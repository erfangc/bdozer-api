package com.starburst.starburst.edgar.factbase.modelbuilder

import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model

/**
 * This is the brains of the automated valuation model
 */
class ModelFormulaBuilder(
    private val model: Model,
    private val ctx: ModelFormulaBuilderContext
) {

    /**
     * Takes as input model that is already linked via the calculationArcs
     * and with historical values for the items populated
     *
     * This is the master method for which we begin to populate formulas and move them beyond
     * simply 0.0 or repeating historical
     */
    fun buildModelFormula(): Model {

        val formualtedIncomeStatementItems = model.incomeStatementItems.map { item ->
            /*
            for any items for which there exists an existing calculationArc specified
            calculation return it without further processing
             */
            val formulatedItem = when {
                ctx.itemDependencyGraph[item.name]
                    ?.isNotEmpty() == true -> {
                    item
                }
                /*
                treat revenue item(s) using revenue like formulas
                 */
                isRevenue(item) -> {
                    formulateRevenueItem(item)
                }
                /*
                if this is a cost item that should be revenue driven then
                process it using [formulateRevenueDrivenItem]
                 */
                isRevenueDriven(item) -> {
                    formulateRevenueDrivenItem(item)
                }
                isTotalAssetDriven(item) -> {
                    formulateTotalAssetDrivenItem(item)
                }
                isFixed(item) -> {
                    formulateFixedCostItem(item)
                }
                isOneTime(item) -> {
                    formulateOneTimeItem(item)
                }
                else -> {
                    item
                }
            }

            /*
            Determine if the item is something that requires some additional process
            and flagging

            Here we use private extension classes to chain them linearly rather than
            nesting the processing

            Taking advantage of a great but controversial Kotlin lang feature
            for readability - please do not export these chain functions, as they make no semantic sense
            outside the context of this class
             */
            formulatedItem
                .stockBasedCompensationChain()
                .nonCashChain()
        }

        return model
    }

    private fun Item.stockBasedCompensationChain(): Item {
        return if (this.name == "ShareBasedCompensation") {
            this.copy(stockBasedCompensation = true)
        } else {
            this
        }
    }

    private fun Item.nonCashChain(): Item {
        val itemName = this.name

        // we find the element definition
        val elementDefinition = ctx.elementDefinitionMap[itemName]
        val balance = elementDefinition?.balance
        val abstract = elementDefinition?.abstract
        val periodType = elementDefinition?.periodType
        val type = elementDefinition?.type

        /*
        A non cash expense is one that
        1. is a non-abstract monetary debit item as defined by the company's XSD or us-gaap
        2. starts or ends with the correct keywords
         */
        val isFlowItem = balance == "debit"
                && abstract != true
                && periodType == "duration"
                && type?.endsWith("monetaryItemType") == true

        val hasDesiredKeyword = if (isFlowItem) {
            // next test whether it matches the right vocab
            val keywords = listOf("amortization", "depreciation", "impairment")
            keywords.any { keyword ->
                itemName.toLowerCase().startsWith(keyword)
                        || itemName.toLowerCase().endsWith(keyword)
            }
        } else {
            false
        }

        return if (isFlowItem && hasDesiredKeyword) {
            this.copy(nonCashExpense = true)
        } else {
            this
        }
    }

    private fun formulateFixedCostItem(item: Item): Item {
        return item.copy(expression = "${item.historicalValue}")
    }

    private fun isFixed(item: Item): Boolean {
        /*
        An item is considered a fixed expense if it does not appear to vary at all with revenue
         */
        expressionForTotalRevenue()
        TODO("Not yet implemented")
    }

    private fun formulateOneTimeItem(item: Item): Item {
        TODO()
    }

    private fun isOneTime(item: Item): Boolean {
        TODO("Not yet implemented")
    }

    private fun isRevenue(item: Item): Boolean {
        // for now assume that any balance=credit is a revenue item
        val balance = ctx.elementDefinitionMap[item.name]?.balance
        val periodType = ctx.elementDefinitionMap[item.name]?.periodType
        val type = ctx.elementDefinitionMap[item.name]?.type ?: ""
        return balance == "credit" && periodType == "duration" && type.endsWith("monetaryItemType")
    }

    /**
     * Figure out the correct [Item.expression] for total revenue for this company
     * if a total revenue line is already defined, then use that otherwise create a summation expression
     * of the individual components
     */
    private fun expressionForTotalRevenue(): String {
        // see if the standard set of GAAP total revenue metrics are defined
        return listOf(
            "Revenues",
            "RevenueFromContractWithCustomerExcludingAssessedTax",
            "RevenuesNetOfInterestExpense",
        ).find { itemName -> ctx.itemDependencyGraph.containsKey(itemName) && ctx.itemDependencyGraph[itemName]!!.isNotEmpty() } ?: error("Unable to find a total revenue item")
    }

    private fun formulateRevenueItem(item: Item): Item {
        TODO()
    }

    private fun isRevenueDriven(item: Item): Boolean {
        TODO()
    }

    private fun formulateRevenueDrivenItem(item: Item): Item {
        TODO()
    }

    private fun isTotalAssetDriven(item: Item): Boolean {
        TODO()
    }

    private fun formulateTotalAssetDrivenItem(item: Item): Item {
        TODO()
    }

}