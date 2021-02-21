package com.starburst.starburst.models.builders

import com.starburst.starburst.cells.Cell
import com.starburst.starburst.cells.evaluation.CellEvaluator
import com.starburst.starburst.computers.ReservedItemNames.CapitalExpenditure
import com.starburst.starburst.computers.ReservedItemNames.ChangeInWorkingCapital
import com.starburst.starburst.computers.ReservedItemNames.CostOfGoodsSold
import com.starburst.starburst.computers.ReservedItemNames.CurrentAsset
import com.starburst.starburst.computers.ReservedItemNames.CurrentLiability
import com.starburst.starburst.computers.ReservedItemNames.DepreciationAmortization
import com.starburst.starburst.computers.ReservedItemNames.FreeCashFlow
import com.starburst.starburst.computers.ReservedItemNames.GrossProfit
import com.starburst.starburst.computers.ReservedItemNames.InterestExpense
import com.starburst.starburst.computers.ReservedItemNames.NetIncome
import com.starburst.starburst.computers.ReservedItemNames.NonOperatingExpense
import com.starburst.starburst.computers.ReservedItemNames.OperatingExpense
import com.starburst.starburst.computers.ReservedItemNames.OperatingIncome
import com.starburst.starburst.computers.ReservedItemNames.Revenue
import com.starburst.starburst.computers.ReservedItemNames.StockBasedCompensation
import com.starburst.starburst.computers.ReservedItemNames.TaxExpense
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.builders.SkeletonModel.skeletonModel
import com.starburst.starburst.models.translator.CellExpressionResolver
import com.starburst.starburst.models.translator.ModelToCellTranslator
import org.springframework.stereotype.Service

@Service
class ModelBuilder {

    private val modelToCellTranslator = ModelToCellTranslator()

    /**
     * This method enhances the user provided [Model] to become a fully formed
     * [Model] that is ready to be evaluated. This means mandatory [Item] are added
     * and their relationships automatically defined and linkage established
     */
    fun reformulateModel(model: Model): Model {

        // Step 1 - calculate revenue
        val incomeStatementItems = model.incomeStatementItems
        fun idxOf(name: String): Int {
            return incomeStatementItems.indexOfFirst { it.name == name }
        }

        val revenueIdx = idxOf(Revenue)
        val revenueItems = incomeStatementItems.subList(0, revenueIdx)
        val revenueSubtotal =
            incomeStatementItems[revenueIdx].copy(expression = revenueItems.joinToString("+") { it.name })

        // Step 2 - calculate cost of goods sold
        val cogsIdx = idxOf(CostOfGoodsSold)
        val cogsItems = incomeStatementItems.subList(revenueIdx + 1, cogsIdx)
        val cogsSubtotal =
            incomeStatementItems[cogsIdx].copy(expression = cogsItems.joinToString("+") { it.name })

        //
        // Step 3 - calculate gross profit
        //
        val grossProfitIdx = idxOf(GrossProfit)
        val grossProfitSubtotal = incomeStatementItems[grossProfitIdx].copy(expression = "$Revenue-$CostOfGoodsSold")

        //
        // Step 4 - calculate operating expenses
        //
        val opExpIdx = idxOf(OperatingExpense)
        val opExpItems = incomeStatementItems.subList(grossProfitIdx + 1, opExpIdx)
        val opExpSubtotal = incomeStatementItems[opExpIdx].copy(expression = opExpItems.joinToString("+") { it.name })

        //
        // Step 5 - calculate operating income
        //
        val opIncIdx = idxOf(OperatingIncome)
        val opIncSubtotal = incomeStatementItems[opIncIdx].copy(expression = "$GrossProfit-$OperatingExpense")

        //
        // Step 6 - calculate non-operating expenses
        //
        val nonOpExpIdx = idxOf(NonOperatingExpense)
        val nonOpExpItems = incomeStatementItems.subList(opIncIdx + 1, nonOpExpIdx)
        val nonOpExpSubtotal =
            incomeStatementItems[nonOpExpIdx].copy(expression = nonOpExpItems.joinToString("+") { it.name })

        //
        // Step 7 - calculate interest/tax expenses
        // TODO actually do something here
        val intExpIdx = idxOf(InterestExpense)
        val intExpItem = incomeStatementItems[intExpIdx]

        val taxExpenseIdx = idxOf(TaxExpense)
        val taxExpenseItem = incomeStatementItems[taxExpenseIdx]

        //
        // Step 8 - calculate net income
        //
        val netIncomeIdx = idxOf(NetIncome)
        val netIncomeSubtotal = incomeStatementItems[netIncomeIdx].copy(
            expression = "$OperatingIncome-$NonOperatingExpense-$InterestExpense-$TaxExpense"
        )

        //
        // Step 9 - calculate CAPEX
        // TODO go through the drivers and figure out which ones require CAPEX adjustements
        val capexItem = Item(
            name = CapitalExpenditure,
            expression = "0"
        )

        //
        // Step 10 - calculate depreciation & amortization
        // TODO go through the drivers and figure out which ones require depreciation & amortization adjustment
        val daItem = Item(
            name = DepreciationAmortization,
            expression = "0"
        )

        //
        // Step 11 - calculate stock based compensation
        // TODO go through the drivers and figure out which ones is SBC - create the formula
        val sbcItem = Item(
            name = StockBasedCompensation,
            expression = "0"
        )

        //
        // Step 12 - calculate balance sheet impact
        //
        // TODO
        val changeInWorkingCapitalItem = Item(
            name = ChangeInWorkingCapital,
            description = "Change in Working Capital",
        //  expression = "($CurrentAsset-$CurrentLiability)-(${previous(CurrentAsset)}-${previous(CurrentLiability)})"
            expression = "0"
        )

        //
        // Step 13 - calculate free cash flow
        //
        val fcfItem = Item(
            name = FreeCashFlow,
            description = "Free Cash Flow",
            expression = "$NetIncome-$CapitalExpenditure+$DepreciationAmortization+$StockBasedCompensation-$ChangeInWorkingCapital"
        )

        val incomeStatementItemsNew = revenueItems +
                revenueSubtotal +
                cogsItems +
                cogsSubtotal +
                grossProfitSubtotal +
                opExpItems +
                opExpSubtotal +
                opIncSubtotal +
                nonOpExpItems +
                nonOpExpSubtotal +
                intExpItem +
                taxExpenseItem +
                netIncomeSubtotal

        val otherItems = listOf(
            capexItem,
            daItem,
            sbcItem,
            changeInWorkingCapitalItem,
            fcfItem
        )

        return model.copy(incomeStatementItems = incomeStatementItemsNew, otherItems = otherItems)
    }

    /**
     * Create cell name that refers to [previous] instance of an item
     */
    private fun previous(name: String): String {
        return "Previous_$name"
    }

    /**
     * [createModel] creates the skeleton of a basic model
     * this doesn't have to be the only skeleton model available
     */
    fun createModel(): Model {
        return skeletonModel
    }

    /**
     * Evaluates a user provided model, prior to evaluation [reformulateModel] will be invoked
     * to ensure the model is in good form
     */
    fun evaluateModel(model: Model): List<Cell> {
        val fullyFormedModel = reformulateModel(model)
        val generateCells = modelToCellTranslator.generateCells(fullyFormedModel)
        val cells = CellExpressionResolver().resolveCellExpressions(fullyFormedModel, generateCells)
        return CellEvaluator().evaluate(fullyFormedModel, cells)
    }

}
