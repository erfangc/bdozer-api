import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.FormulaGenerator
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.Result
import com.starburst.starburst.models.Item
import org.springframework.stereotype.Service

@Service
class ItemizedFormulaGenerator : FormulaGenerator {

    override fun generate(item: Item, ctx: ModelFormulaBuilderContext): Result {
        return when (item.name) {
            "AccountsPayableCurrent" -> {
                Result(item = item, commentary = "")
            }
            "AccountsReceivableNetCurrent" -> {
                Result(item = item, commentary = "")
            }
            "AccruedLiabilitiesandOtherLiabilitiesCurrent" -> {
                Result(item = item, commentary = "")
            }
            "AccumulatedOtherComprehensiveIncomeLossNetOfTax" -> {
                Result(item = item, commentary = "")
            }
            "AdditionalPaidInCapital" -> {
                Result(item = item, commentary = "")
            }
            "AssetImpairmentCharges" -> {
                Result(item = item, commentary = "")
            }
            "AvailableForSaleSecuritiesDebtSecuritiesCurrent" -> {
                Result(item = item, commentary = "")
            }
            "CapitalizedContractCostAmortization" -> {
                Result(item = item, commentary = "")
            }
            "CashAndCashEquivalentsAtCarryingValue" -> {
                Result(item = item, commentary = "")
            }
            "CommitmentsAndContingencies" -> {
                Result(item = item, commentary = "")
            }
            "CommonStockValue" -> {
                Result(item = item, commentary = "")
            }
            "ContractWithCustomerLiabilityCurrent" -> {
                Result(item = item, commentary = "")
            }
            "CostOfGoodsAndServicesSold" -> {
                Result(item = item, commentary = "")
            }
            "DeprecationDepletionAndAmortizationExcludingAmortizationOfDeferredSalesCommissions" -> {
                Result(item = item, commentary = "")
            }
            "EffectOfExchangeRateOnCashCashEquivalentsRestrictedCashAndRestrictedCashEquivalents" -> {
                Result(item = item, commentary = "")
            }
            "EmployeeRelatedLiabilitiesCurrent" -> {
                Result(item = item, commentary = "")
            }
            "EquitySecuritiesFvNiGainLoss" -> {
                Result(item = item, commentary = "")
            }
            "FinanceLeaseLiabilityCurrent" -> {
                Result(item = item, commentary = "")
            }
            "FinanceLeaseLiabilityNoncurrent" -> {
                Result(item = item, commentary = "")
            }
            "FinanceLeasePrincipalPayments" -> {
                Result(item = item, commentary = "")
            }
            "GeneralAndAdministrativeExpense" -> {
                Result(item = item, commentary = "")
            }
            "Goodwill" -> {
                Result(item = item, commentary = "")
            }
            "IncomeTaxExpenseBenefit" -> {
                Result(item = item, commentary = "")
            }
            "IncreaseDecreaseInAccountsAndOtherReceivables" -> {
                Result(item = item, commentary = "")
            }
            "IncreaseDecreaseInAccountsPayable" -> {
                Result(item = item, commentary = "")
            }
            "IncreaseDecreaseInAccruedLiabilitiesAndOtherLiabilities" -> {
                Result(item = item, commentary = "")
            }
            "IncreaseDecreaseInContractWithCustomerLiability" -> {
                Result(item = item, commentary = "")
            }
            "IncreaseDecreaseInEmployeeRelatedLiabilities" -> {
                Result(item = item, commentary = "")
            }
            "IncreaseDecreaseInOtherNoncurrentLiabilities" -> {
                Result(item = item, commentary = "")
            }
            "IncreaseDecreaseInOtherOperatingAssets" -> {
                Result(item = item, commentary = "")
            }
            "IncreaseDecreaseInPrepaidDeferredExpenseAndOtherAssets" -> {
                Result(item = item, commentary = "")
            }
            "IncreaseDecreaseTenantImprovementAllowanceReimbursement" -> {
                Result(item = item, commentary = "")
            }
            "IntangibleAssetsNetExcludingGoodwill" -> {
                Result(item = item, commentary = "")
            }
            "InterestExpense" -> {
                Result(item = item, commentary = "")
            }
            "InterestIncomeExpenseNonoperatingNet" -> {
                Result(item = item, commentary = "")
            }
            "OperatingLeaseLiabilityCurrent" -> {
                Result(item = item, commentary = "")
            }
            "OperatingLeaseLiabilityNoncurrent" -> {
                Result(item = item, commentary = "")
            }
            "OperatingLeaseRightOfUseAsset" -> {
                Result(item = item, commentary = "")
            }
            "OtherAssetsNoncurrent" -> {
                Result(item = item, commentary = "")
            }
            "OtherLiabilitiesNoncurrent" -> {
                Result(item = item, commentary = "")
            }
            "OtherNoncashIncomeExpense" -> {
                Result(item = item, commentary = "")
            }
            "OtherNonoperatingIncomeExpense" -> {
                Result(item = item, commentary = "")
            }
            "PaymentsForProceedsFromOtherInvestingActivities" -> {
                Result(item = item, commentary = "")
            }
            "PaymentsForRepurchaseOfCommonStock" -> {
                Result(item = item, commentary = "")
            }
            "PaymentsOfStockIssuanceCosts" -> {
                Result(item = item, commentary = "")
            }
            "PaymentsRelatedToTaxWithholdingForShareBasedCompensation" -> {
                Result(item = item, commentary = "")
            }
            "PaymentsToAcquireAvailableForSaleSecuritiesDebt" -> {
                Result(item = item, commentary = "")
            }
            "PaymentsToAcquireBusinessesNetOfCashAcquired" -> {
                Result(item = item, commentary = "")
            }
            "PaymentsToAcquireIntangibleAssets" -> {
                Result(item = item, commentary = "")
            }
            "PaymentsToAcquirePropertyPlantAndEquipment" -> {
                Result(item = item, commentary = "")
            }
            "PreferredStockValue" -> {
                Result(item = item, commentary = "")
            }
            "PrepaidExpenseAndOtherAssetsCurrent" -> {
                Result(item = item, commentary = "")
            }
            "ProceedsFromIssuanceInitialPublicOffering" -> {
                Result(item = item, commentary = "")
            }
            "ProceedsFromIssuanceOfCommonStock" -> {
                Result(item = item, commentary = "")
            }
            "ProceedsFromMaturitiesPrepaymentsAndCallsOfShorttermInvestments" -> {
                Result(item = item, commentary = "")
            }
            "ProceedsFromPaymentsForOtherFinancingActivities" -> {
                Result(item = item, commentary = "")
            }
            "ProceedsFromSaleOfShortTermInvestments" -> {
                Result(item = item, commentary = "")
            }
            "PropertyPlantAndEquipmentNet" -> {
                Result(item = item, commentary = "")
            }
            "ResearchAndDevelopmentExpense" -> {
                Result(item = item, commentary = "")
            }
            "RetainedEarningsAccumulatedDeficit" -> {
                Result(item = item, commentary = "")
            }
            "RevenueFromContractWithCustomerExcludingAssessedTax" -> {
                Result(item = item, commentary = "")
            }
            "SellingAndMarketingExpense" -> {
                Result(item = item, commentary = "")
            }
            "ShareBasedCompensation" -> {
                Result(item = item, commentary = "")
            }
            else -> Result(item = item)
        }

    }

    override fun relevantForItem(item: Item, ctx: ModelFormulaBuilderContext): Boolean {
        TODO("Not yet implemented")
    }
}