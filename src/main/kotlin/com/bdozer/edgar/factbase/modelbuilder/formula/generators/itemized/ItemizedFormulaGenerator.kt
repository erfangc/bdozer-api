package com.bdozer.edgar.factbase.modelbuilder.formula.generators.itemized

import com.bdozer.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.bdozer.edgar.factbase.modelbuilder.formula.generators.FormulaGenerator
import com.bdozer.edgar.factbase.modelbuilder.formula.generators.Result
import com.bdozer.edgar.factbase.modelbuilder.formula.generators.generalized.AverageFormulaGenerator
import com.bdozer.edgar.factbase.modelbuilder.formula.generators.generalized.OneTimeExpenseGenerator
import com.bdozer.edgar.factbase.modelbuilder.formula.generators.generalized.PercentOfRevenueFormulaGenerator
import com.bdozer.edgar.factbase.modelbuilder.formula.generators.generalized.RevenueDrivenFormulaGenerator
import com.bdozer.models.dataclasses.Item
import org.springframework.stereotype.Service

@Service
class ItemizedFormulaGenerator(
    /*
    Itemized formula generators
     */
    private val revenueFormulaGenerator: RevenueFormulaGenerator,
    private val interestFormulaGenerator: InterestFormulaGenerator,
    private val taxExpenseFormulaGenerator: TaxExpenseFormulaGenerator,
    private val stockBasedCompensationGenerator: StockBasedCompensationGenerator,

    /*
    Generalized formula generators
     */
    private val averageFormulaGenerator: AverageFormulaGenerator,
    private val oneTimeExpenseGenerator: OneTimeExpenseGenerator,
    private val percentOfRevenueFormulaGenerator: PercentOfRevenueFormulaGenerator,
    private val revenueDrivenFormulaGenerator: RevenueDrivenFormulaGenerator,
) : FormulaGenerator {

    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
        return when (item.name) {
            "AccountsPayableCurrent" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "AccountsReceivableNetCurrent" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "AccruedLiabilitiesandOtherLiabilitiesCurrent" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "AccumulatedOtherComprehensiveIncomeLossNetOfTax" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "AdditionalPaidInCapital" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "AssetImpairmentCharges" -> {
                oneTimeExpenseGenerator.generate(item, ctx)
            }
            "AvailableForSaleSecuritiesDebtSecuritiesCurrent" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "CapitalizedContractCostAmortization" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "CashAndCashEquivalentsAtCarryingValue" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "CommitmentsAndContingencies" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "CommonStockValue" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "ContractWithCustomerLiabilityCurrent" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "CostOfGoodsAndServicesSold" -> {
                percentOfRevenueFormulaGenerator.generate(item, ctx)
            }
            "DeprecationDepletionAndAmortizationExcludingAmortizationOfDeferredSalesCommissions" -> {
                percentOfRevenueFormulaGenerator.generate(item, ctx)
            }
            "EffectOfExchangeRateOnCashCashEquivalentsRestrictedCashAndRestrictedCashEquivalents" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "EmployeeRelatedLiabilitiesCurrent" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "EquitySecuritiesFvNiGainLoss" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "FinanceLeaseLiabilityCurrent" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "FinanceLeaseLiabilityNoncurrent" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "FinanceLeasePrincipalPayments" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "GeneralAndAdministrativeExpense" -> {
                revenueDrivenFormulaGenerator.generate(item, ctx)
            }
            "Goodwill" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "IncomeTaxExpenseBenefit" -> {
                taxExpenseFormulaGenerator.generate(item, ctx)
            }
            "IncreaseDecreaseInAccountsAndOtherReceivables" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "IncreaseDecreaseInAccountsPayable" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "IncreaseDecreaseInAccruedLiabilitiesAndOtherLiabilities" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "IncreaseDecreaseInContractWithCustomerLiability" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "IncreaseDecreaseInEmployeeRelatedLiabilities" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "IncreaseDecreaseInOtherNoncurrentLiabilities" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "IncreaseDecreaseInOtherOperatingAssets" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "IncreaseDecreaseInPrepaidDeferredExpenseAndOtherAssets" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "IncreaseDecreaseTenantImprovementAllowanceReimbursement" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "IntangibleAssetsNetExcludingGoodwill" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "InterestExpense" -> {
                interestFormulaGenerator.generate(item, ctx)
            }
            "InterestIncomeExpenseNonoperatingNet" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "OperatingLeaseLiabilityCurrent" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "OperatingLeaseLiabilityNoncurrent" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "OperatingLeaseRightOfUseAsset" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "OtherAssetsNoncurrent" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "OtherLiabilitiesNoncurrent" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "OtherNoncashIncomeExpense" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "OtherNonoperatingIncomeExpense" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "PaymentsForProceedsFromOtherInvestingActivities" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "PaymentsForRepurchaseOfCommonStock" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "PaymentsOfStockIssuanceCosts" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "PaymentsRelatedToTaxWithholdingForShareBasedCompensation" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "PaymentsToAcquireAvailableForSaleSecuritiesDebt" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "PaymentsToAcquireBusinessesNetOfCashAcquired" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "PaymentsToAcquireIntangibleAssets" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "PaymentsToAcquirePropertyPlantAndEquipment" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "PreferredStockValue" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "PrepaidExpenseAndOtherAssetsCurrent" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "ProceedsFromIssuanceInitialPublicOffering" -> {
                oneTimeExpenseGenerator.generate(item, ctx)
            }
            "ProceedsFromIssuanceOfCommonStock" -> {
                oneTimeExpenseGenerator.generate(item, ctx)
            }
            "ProceedsFromMaturitiesPrepaymentsAndCallsOfShorttermInvestments" -> {
                oneTimeExpenseGenerator.generate(item, ctx)
            }
            "ProceedsFromPaymentsForOtherFinancingActivities" -> {
                oneTimeExpenseGenerator.generate(item, ctx)
            }
            "ProceedsFromSaleOfShortTermInvestments" -> {
                oneTimeExpenseGenerator.generate(item, ctx)
            }
            "PropertyPlantAndEquipmentNet" -> {
                revenueDrivenFormulaGenerator.generate(item, ctx)
            }
            "ResearchAndDevelopmentExpense" -> {
                revenueDrivenFormulaGenerator.generate(item, ctx)
            }
            "RetainedEarningsAccumulatedDeficit" -> {
                averageFormulaGenerator.generate(item, ctx)
            }
            "RevenueFromContractWithCustomerExcludingAssessedTax" -> {
                revenueFormulaGenerator.generate(item, ctx)
            }
            "SellingAndMarketingExpense" -> {
                revenueDrivenFormulaGenerator.generate(item, ctx)
            }
            "ShareBasedCompensation" -> {
                stockBasedCompensationGenerator.generate(item, ctx)
            }
            else -> Result(item = item)
        }

    }

    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        return false
    }
}