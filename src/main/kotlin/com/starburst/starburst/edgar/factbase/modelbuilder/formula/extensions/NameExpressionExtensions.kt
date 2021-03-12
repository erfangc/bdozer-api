package com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.models.Item

/**
 * This is the brains of the automated valuation model
 */
internal object NameExpressionExtensions {

    /**
     * Figure out the correct [Item.expression] for total revenue for this company
     * if a total revenue line is already defined, then use that otherwise create a summation expression
     * of the individual components
     */
    fun ModelFormulaBuilderContext.totalRevenueExpression(): String {
        // see if the standard set of GAAP total revenue metrics are defined
        // TODO be more rigorous here
        return listOf(
            "Revenues",
            "RevenueFromContractWithCustomerExcludingAssessedTax",
            "RevenuesNetOfInterestExpense",
        )
            .find { itemName -> itemDependencyGraph.containsKey(itemName) }
            ?: error("Unable to find a total revenue item")
    }

}