package com.starburst.starburst.models.builders

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
import com.starburst.starburst.spreadsheet.evaluation.CellEvaluator
import org.springframework.stereotype.Service

@Service
class ModelBuilder {

    private val modelToCellTranslator = ModelToCellTranslator()

    private fun sanitize(formula: String): String {
        // if the formula is empty then return 0
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

        //
        // Step 1 - calculate revenue
        //
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
            incomeStatementItems[revenueIdx].copy(
                expression = sanitize(revenueItems.joinToString("+") { it.name }),
                historicalValue = revenueItems.sumByDouble { it.historicalValue }
            )

        //
        // Step 2 - calculate cost of goods sold
        //
        val cogsIdx = idxOfInc(CostOfGoodsSold)
        val cogsItems = incomeStatementItems.subList(revenueIdx + 1, cogsIdx)
        val cogsSubtotal =
            incomeStatementItems[cogsIdx].copy(
                expression = sanitize(cogsItems.joinToString("+") { it.name }),
                historicalValue = cogsItems.sumByDouble { it.historicalValue }
            )

        //
        // Step 3 - calculate gross profit
        //
        val grossProfitIdx = idxOfInc(GrossProfit)
        val grossProfitSubtotal = incomeStatementItems[grossProfitIdx].copy(
            expression = "$Revenue-$CostOfGoodsSold",
            historicalValue = revenueSubtotal.historicalValue - cogsSubtotal.historicalValue
        )

        //
        // Step 4 - calculate operating expenses
        //
        val opExpIdx = idxOfInc(OperatingExpense)
        val opExpItems = incomeStatementItems.subList(grossProfitIdx + 1, opExpIdx)
        val opExpSubtotal = incomeStatementItems[opExpIdx].copy(
            expression = sanitize(opExpItems.joinToString("+") { it.name }),
            historicalValue = opExpItems.sumByDouble { it.historicalValue }
        )

        //
        // Step 5 - calculate operating income
        //
        val opIncIdx = idxOfInc(OperatingIncome)
        val opIncSubtotal = incomeStatementItems[opIncIdx].copy(
            expression = "$GrossProfit-$OperatingExpense",
            historicalValue = opExpItems.sumByDouble { it.historicalValue }
        )

        //
        // Step 6 - calculate non-operating expenses
        //
        val nonOpExpIdx = idxOfInc(NonOperatingExpense)
        val nonOpExpItems = incomeStatementItems.subList(opIncIdx + 1, nonOpExpIdx)
        val nonOpExpSubtotal =
            incomeStatementItems[nonOpExpIdx].copy(
                expression = sanitize(nonOpExpItems.joinToString("+") { it.name }),
                historicalValue = nonOpExpItems.sumByDouble { it.historicalValue }
            )

        //
        // Step 7 - calculate interest/tax expenses
        // TODO actually do something here
        val intExpIdx = idxOfInc(InterestExpense)
        val intExpItem = incomeStatementItems[intExpIdx]

        val taxExpenseIdx = idxOfInc(TaxExpense)
        val taxExpenseItem = incomeStatementItems[taxExpenseIdx]

        //
        // Step 8 - calculate net income
        //
        val netIncomeIdx = idxOfInc(NetIncome)
        val netIncomeSubtotal = incomeStatementItems[netIncomeIdx].copy(
            expression = "$OperatingIncome-$NonOperatingExpense-$InterestExpense-$TaxExpense",
            historicalValue = opIncSubtotal.historicalValue -
                    nonOpExpSubtotal.historicalValue -
                    intExpItem.historicalValue -
                    taxExpenseItem.historicalValue
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

        val caIdx = idxOfBal(CurrentAsset)
        val caItems = balanceSheetItems.subList(0, caIdx)
        val caItem = balanceSheetItems[caIdx].copy(
            expression = if (caItems.isEmpty())
                previous(CurrentAsset)
            else caItems.joinToString("+") { it.name },
            historicalValue = caItems.sumByDouble { it.historicalValue }
        )

        val ltaIdx = idxOfBal(LongTermAsset)
        val ltaItems = balanceSheetItems.subList(caIdx + 1, ltaIdx)
        val ltaItem = balanceSheetItems[ltaIdx].copy(
            expression = if (ltaItems.isEmpty())
                previous(LongTermAsset)
            else
                ltaItems.joinToString("+") { it.name },
            historicalValue = ltaItems.sumByDouble { it.historicalValue }
        )

        val taIdx = idxOfBal(TotalAsset)
        val taItem = balanceSheetItems[taIdx].copy(
            expression = "$CurrentAsset+$LongTermAsset",
            historicalValue = caItem.historicalValue + ltaItem.historicalValue
        )

        val clIdx = idxOfBal(CurrentLiability)
        val clItems = balanceSheetItems.subList(taIdx + 1, clIdx)
        val clItem = balanceSheetItems[clIdx].copy(
            expression = if (clItems.isEmpty())
                previous(CurrentLiability)
            else
                clItems.joinToString("+") { it.name },
            historicalValue = clItems.sumByDouble { it.historicalValue }
        )

        val ltlIdx = idxOfBal(LongTermLiability)
        val ltlItems = balanceSheetItems.subList(clIdx + 1, ltlIdx)
        val ltlItem = balanceSheetItems[ltlIdx].copy(
            expression = if (ltlItems.isEmpty())
                previous(LongTermLiability)
            else
                ltlItems.joinToString("+") { it.name },
            historicalValue = ltlItems.sumByDouble { it.historicalValue }
        )

        val ttlIdx = idxOfBal(TotalLiability)
        val tl = balanceSheetItems[ttlIdx].copy(
            expression = "$CurrentLiability+$LongTermLiability",
            historicalValue = clItem.historicalValue + ltlItem.historicalValue
        )

        val equityIdx = idxOfBal(ShareholdersEquity)
        val equityItem = balanceSheetItems[equityIdx].copy(
            expression = "$TotalAsset-$TotalLiability",
            historicalValue = taItem.historicalValue - tl.historicalValue
        )

        val changeInWorkingCapitalItem = Item(
            name = ChangeInWorkingCapital,
            description = "Change in Working Capital",
            expression = "($CurrentAsset-$CurrentLiability)-(${previous(CurrentAsset)}-${previous(CurrentLiability)})"
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
            // TODO calculate actual shares outstanding based on convertibles, SBCs
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

        val balanceSheetItemsNew = caItems +
                caItem +
                ltaItems +
                ltaItem +
                taItem +
                clItems +
                clItem +
                ltlItems +
                ltlItem +
                tl +
                equityItem

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
