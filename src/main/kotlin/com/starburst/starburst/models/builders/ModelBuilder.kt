package com.starburst.starburst.models.builders

import com.starburst.starburst.spreadsheet.evaluation.CellEvaluator
import com.starburst.starburst.computers.ReservedItemNames.CapitalExpenditure
import com.starburst.starburst.computers.ReservedItemNames.ChangeInWorkingCapital
import com.starburst.starburst.computers.ReservedItemNames.CostOfGoodsSold
import com.starburst.starburst.computers.ReservedItemNames.CurrentAsset
import com.starburst.starburst.computers.ReservedItemNames.CurrentLiability
import com.starburst.starburst.computers.ReservedItemNames.DepreciationAmortization
import com.starburst.starburst.computers.ReservedItemNames.FreeCashFlow
import com.starburst.starburst.computers.ReservedItemNames.GrossProfit
import com.starburst.starburst.computers.ReservedItemNames.InterestExpense
import com.starburst.starburst.computers.ReservedItemNames.LongTermAsset
import com.starburst.starburst.computers.ReservedItemNames.LongTermLiability
import com.starburst.starburst.computers.ReservedItemNames.NetIncome
import com.starburst.starburst.computers.ReservedItemNames.NonOperatingExpense
import com.starburst.starburst.computers.ReservedItemNames.OperatingExpense
import com.starburst.starburst.computers.ReservedItemNames.OperatingIncome
import com.starburst.starburst.computers.ReservedItemNames.Revenue
import com.starburst.starburst.computers.ReservedItemNames.ShareholdersEquity
import com.starburst.starburst.computers.ReservedItemNames.SharesOutstanding
import com.starburst.starburst.computers.ReservedItemNames.StockBasedCompensation
import com.starburst.starburst.computers.ReservedItemNames.TaxExpense
import com.starburst.starburst.computers.ReservedItemNames.TotalAsset
import com.starburst.starburst.computers.ReservedItemNames.TotalLiability
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.ModelEvaluationOutput
import com.starburst.starburst.models.Util.previous
import com.starburst.starburst.models.builders.SkeletonModel.skeletonModel
import com.starburst.starburst.models.translator.CellFormulaTranslator
import com.starburst.starburst.models.translator.ModelToCellTranslator
import com.starburst.starburst.pv.DcfCalculator
import org.springframework.stereotype.Service

@Service
class ModelBuilder {

    private val modelToCellTranslator = ModelToCellTranslator()

    private fun sanitize(formula: String): String {
        // if the formual is empty then return 0
        return if (formula.isEmpty()) {
            "0.0"
        } else {
            formula
        }
    }

    /**
     * This method enhances the user provided [Model] to become a fully formed
     * [Model] that is ready to be evaluated. This means mandatory [Item] are added
     * and their relationships automatically defined and linkage established
     */
    fun reformulateModel(model: Model): Model {

        // Step 1 - calculate revenue
        val incomeStatementItems = model.incomeStatementItems
        val balanceSheetItems = model.balanceSheetItems

        /**
         * a helper function to get the idx of an item by a certain name
         */
        fun idxOfBal(name: String): Int {
            return balanceSheetItems.indexOfFirst { it.name == name }
        }

        fun idxOfInc(name: String): Int {
            return incomeStatementItems.indexOfFirst { it.name == name }
        }

        val revenueIdx = idxOfInc(Revenue)
        val revenueItems = incomeStatementItems.subList(0, revenueIdx)
        val revenueSubtotal =
            incomeStatementItems[revenueIdx].copy(expression = sanitize(revenueItems.joinToString("+") { it.name }))

        // Step 2 - calculate cost of goods sold
        val cogsIdx = idxOfInc(CostOfGoodsSold)
        val cogsItems = incomeStatementItems.subList(revenueIdx + 1, cogsIdx)
        val cogsSubtotal =
            incomeStatementItems[cogsIdx].copy(expression = sanitize(cogsItems.joinToString("+") { it.name }))

        //
        // Step 3 - calculate gross profit
        //
        val grossProfitIdx = idxOfInc(GrossProfit)
        val grossProfitSubtotal = incomeStatementItems[grossProfitIdx].copy(expression = "$Revenue-$CostOfGoodsSold")

        //
        // Step 4 - calculate operating expenses
        //
        val opExpIdx = idxOfInc(OperatingExpense)
        val opExpItems = incomeStatementItems.subList(grossProfitIdx + 1, opExpIdx)
        val opExpSubtotal = incomeStatementItems[opExpIdx].copy(expression = sanitize(opExpItems.joinToString("+") { it.name }))

        //
        // Step 5 - calculate operating income
        //
        val opIncIdx = idxOfInc(OperatingIncome)
        val opIncSubtotal = incomeStatementItems[opIncIdx].copy(expression = "$GrossProfit-$OperatingExpense")

        //
        // Step 6 - calculate non-operating expenses
        //
        val nonOpExpIdx = idxOfInc(NonOperatingExpense)
        val nonOpExpItems = incomeStatementItems.subList(opIncIdx + 1, nonOpExpIdx)
        val nonOpExpSubtotal =
            incomeStatementItems[nonOpExpIdx].copy(expression = sanitize(nonOpExpItems.joinToString("+") { it.name }))

        //
        // Step 7 - calculate interest/tax expenses
        // TODO actually do something here
        val intExpIdx = idxOfInc(InterestExpense)
        val intExpItem = if (incomeStatementItems[intExpIdx].expression == null) {
            incomeStatementItems[intExpIdx].copy(expression = "0.0")
        } else {
            incomeStatementItems[intExpIdx]
        }

        val taxExpenseIdx = idxOfInc(TaxExpense)
        val taxExpenseItem = if (incomeStatementItems[taxExpenseIdx].expression == null) {
            incomeStatementItems[taxExpenseIdx].copy(expression = "${model.corporateTaxRate}*($OperatingIncome-$NonOperatingExpense-$InterestExpense)")
        } else {
            incomeStatementItems[taxExpenseIdx]
        }

        //
        // Step 8 - calculate net income
        //
        val netIncomeIdx = idxOfInc(NetIncome)
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

        // ---------------------
        // Balance sheet updates
        // ---------------------

        //
        // TODO actually introduce expressions instead of carrying over the previous balance
        //
        val caIdx = idxOfBal(CurrentAsset)
        val caItem = balanceSheetItems[caIdx].copy(expression = previous(CurrentAsset))

        val ltaIdx = idxOfBal(LongTermAsset)
        val ltaItem = balanceSheetItems[ltaIdx].copy(expression = previous(LongTermAsset))

        val taIdx = idxOfBal(TotalAsset)
        val taItem = balanceSheetItems[taIdx].copy(expression = previous(TotalAsset))

        val clIdx = idxOfBal(CurrentLiability)
        val clItem = balanceSheetItems[clIdx].copy(expression = previous(CurrentLiability))

        val ltlIdx = idxOfBal(LongTermLiability)
        val ltlItem = balanceSheetItems[ltlIdx].copy(expression = previous(LongTermLiability))

        val ttlIdx = idxOfBal(TotalLiability)
        val ttlItem = balanceSheetItems[ttlIdx].copy(expression = previous(TotalLiability))

        val equityIdx = idxOfBal(ShareholdersEquity)
        val equityItem = balanceSheetItems[equityIdx].copy(expression = previous(ShareholdersEquity))


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

        val sharesOutstandingItem = Item(
            name = SharesOutstanding,
            description = "Shares Outstanding",
            // TODO calculate actual shares outstanding
            expression = model.sharesOutstanding?.toString() ?: "1.0"
        )

        //
        // TODO compute the diluted number of shares to derive at enterprise value / share
        //
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
            fcfItem,
            sharesOutstandingItem
        )

        val balanceSheetItemsNew = listOf(
            caItem,
            ltaItem,
            taItem,
            clItem,
            ltlItem,
            ttlItem,
            equityItem
        )

        return model.copy(
            incomeStatementItems = incomeStatementItemsNew,
            balanceSheetItems = balanceSheetItemsNew,
            otherItems = otherItems
        )
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
    fun evaluateModel(model: Model): ModelEvaluationOutput {
        val fullyFormedModel = reformulateModel(model)
        val generateCells = modelToCellTranslator.generateCells(fullyFormedModel)
        val cells = CellFormulaTranslator().populateCellsWithFormulas(fullyFormedModel, generateCells)
        val evaluatedCells = CellEvaluator().evaluate(cells)
        return DcfCalculator(fullyFormedModel).calcPv(evaluatedCells)
    }

}
