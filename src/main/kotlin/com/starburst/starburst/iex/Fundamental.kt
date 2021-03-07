package com.starburst.starburst.iex

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Fundamental(
    val accountsPayable: Double? = null,
    val accountsPayableTurnover: Double? = null,
    val accountsReceivable: Double? = null,
    val accountsReceivableTurnover: Double? = null,
    val asOfDate: String? = null,
    val assetsCurrentCash: Double? = null,
    val assetsCurrentCashRestricted: Double? = null,
    val assetsCurrentDeferredCompensation: Double? = null,
    val assetsCurrentDeferredTax: Double? = null,
    val assetsCurrentDiscontinuedOperations: Double? = null,
    val assetsCurrentInvestments: Double? = null,
    val assetsCurrentLeasesOperating: Double? = null,
    val assetsCurrentLoansNet: Double? = null,
    val assetsCurrentOther: Double? = null,
    val assetsCurrentSeparateAccounts: Double? = null,
    val assetsCurrentUnadjusted: Double? = null,
    val assetsFixed: Double? = null,
    val assetsFixedDeferredCompensation: Double? = null,
    val assetsFixedDeferredTax: Double? = null,
    val assetsFixedDiscontinuedOperations: Double? = null,
    val assetsFixedLeasesOperating: Double? = null,
    val assetsFixedOperatingDiscontinuedOperations: Double? = null,
    val assetsFixedOperatingSubsidiaryUnconsolidated: Double? = null,
    val assetsFixedOreo: Double? = null,
    val assetsFixedOther: Double? = null,
    val assetsFixedUnconsolidated: Double? = null,
    val assetsUnadjusted: Double? = null,
    val capex: Double? = null,
    val capexAcquisition: Double? = null,
    val capexMaintenance: Double? = null,
    val cashConversionCycle: Double? = null,
    val cashFlowFinancing: Double? = null,
    val cashFlowInvesting: Double? = null,
    val cashFlowOperating: Double? = null,
    val cashFlowShareRepurchase: Double? = null,
    val cashLongTerm: Double? = null,
    val cashOperating: Double? = null,
    val cashPaidForIncomeTaxes: Double? = null,
    val cashPaidForInterest: Double? = null,
    val cashRestricted: Double? = null,
    val chargeAfterTax: Double? = null,
    val chargeAfterTaxDiscontinuedOperations: Double? = null,
    val chargesAfterTaxOther: Double? = null,
    val cik: String? = null,
    val creditLossProvision: String? = null,
    val dataGenerationDate: String? = null,
    val daysInAccountsPayable: Double? = null,
    val daysInInventory: Double? = null,
    val daysInRevenueDeferred: Double? = null,
    val daysRevenueOutstanding: Double? = null,
    val debtFinancial: Double? = null,
    val debtShortTerm: Double? = null,
    val depreciationAndAmortizationAccumulated: Double? = null,
    val depreciationAndAmortizationCashFlow: Double? = null,
    val dividendsPreferred: Double? = null,
    val dividendsPreferredRedeemableMandatorily: Double? = null,
    val earningsRetained: Double? = null,
    val ebitReported: Double? = null,
    val ebitdaReported: Double? = null,
    val equityShareholder: Double? = null,
    val equityShareholderOther: Double? = null,
    val equityShareholderOtherDeferredCompensation: Double? = null,
    val equityShareholderOtherEquity: Double? = null,
    val equityShareholderOtherMezzanine: Double? = null,
    val expenses: Double? = null,
    val expensesAcquisitionMerger: Double? = null,
    val expensesCompensation: Double? = null,
    val expensesDepreciationAndAmortization: Double? = null,
    val expensesDerivative: Double? = null,
    val expensesDiscontinuedOperations: Double? = null,
    val expensesDiscontinuedOperationsReits: Double? = null,
    val expensesEnergy: Double? = null,
    val expensesForeignCurrency: Double? = null,
    val expensesInterest: Double? = null,
    val expensesInterestFinancials: Double? = null,
    val expensesInterestMinority: Double? = null,
    val expensesLegalRegulatoryInsurance: Double? = null,
    val expensesNonOperatingCompanyDefinedOther: Double? = null,
    val expensesNonOperatingOther: Double? = null,
    val expensesNonOperatingSubsidiaryUnconsolidated: Double? = null,
    val expensesNonRecurringOther: Double? = null,
    val expensesOperating: Double? = null,
    val expensesOperatingOther: Double? = null,
    val expensesOperatingSubsidiaryUnconsolidated: Double? = null,
    val expensesOreo: Double? = null,
    val expensesOreoReits: Double? = null,
    val expensesOtherFinancing: Double? = null,
    val expensesRestructuring: Double? = null,
    val expensesSga: Double? = null,
    val expensesStockCompensation: Double? = null,
    val expensesWriteDown: Double? = null,
    val ffo: Double? = null,
    val figi: String? = null,
    val filingDate: String? = null,
    val filingType: String? = null,
    val fiscalQuarter: Double? = null,
    val fiscalYear: Double? = null,
    val goodwillAmortizationCashFlow: Double? = null,
    val goodwillAmortizationIncomeStatement: Double? = null,
    val goodwillAndIntangiblesNetOther: Double? = null,
    val goodwillNet: Double? = null,
    val incomeFromOperations: Double? = null,
    val incomeNet: Double? = null,
    val incomeNetPerRevenue: Double? = null,
    val incomeNetPerWabso: Double? = null,
    val incomeNetPerWabsoSplitAdjusted: Double? = null,
    val incomeNetPerWabsoSplitAdjustedYoyDeltaPercent: Double? = null,
    val incomeNetPerWadso: Double? = null,
    val incomeNetPerWadsoSplitAdjusted: Double? = null,
    val incomeNetPerWadsoSplitAdjustedYoyDeltaPercent: Double? = null,
    val incomeNetPreTax: Double? = null,
    val incomeNetYoyDelta: Double? = null,
    val incomeOperating: Double? = null,
    val incomeOperatingDiscontinuedOperations: Double? = null,
    val incomeOperatingOther: Double? = null,
    val incomeOperatingSubsidiaryUnconsolidated: Double? = null,
    val incomeOperatingSubsidiaryUnconsolidatedAfterTax: Double? = null,
    val incomeTax: Double? = null,
    val incomeTaxCurrent: Double? = null,
    val incomeTaxDeferred: Double? = null,
    val incomeTaxDiscontinuedOperations: Double? = null,
    val incomeTaxOther: Double? = null,
    val incomeTaxRate: Double? = null,
    val interestMinority: Double? = null,
    val inventory: Double? = null,
    val inventoryTurnover: Double? = null,
    val liabilities: Double? = null,
    val liabilitiesCurrent: Double? = null,
    val liabilitiesNonCurrentAndInterestMinorityTotal: Double? = null,
    val liabilitiesNonCurrentDebt: Double? = null,
    val liabilitiesNonCurrentDeferredCompensation: Double? = null,
    val liabilitiesNonCurrentDeferredTax: Double? = null,
    val liabilitiesNonCurrentDiscontinuedOperations: Double? = null,
    val liabilitiesNonCurrentLeasesOperating: Double? = null,
    val liabilitiesNonCurrentLongTerm: Double? = null,
    val liabilitiesNonCurrentOperatingDiscontinuedOperations: Double? = null,
    val liabilitiesNonCurrentOther: Double? = null,
    val nibclDeferredCompensation: Double? = null,
    val nibclDeferredTax: Double? = null,
    val nibclDiscontinuedOperations: Double? = null,
    val nibclLeasesOperating: Double? = null,
    val nibclOther: Double? = null,
    val nibclRestructuring: Double? = null,
    val nibclRevenueDeferred: Double? = null,
    val nibclRevenueDeferredTurnover: Double? = null,
    val nibclSeparateAccounts: Double? = null,
    val oci: Double? = null,
    val periodEndDate: String? = null,
    val ppAndENet: Double? = null,
    val pricePerEarnings: Double? = null,
    val pricePerEarningsPerRevenueYoyDeltaPercent: Double? = null,
    val profitGross: Double? = null,
    val profitGrossPerRevenue: Double? = null,
    val researchAndDevelopmentExpense: Double? = null,
    val reserves: Double? = null,
    val reservesInventory: Double? = null,
    val reservesLifo: Double? = null,
    val reservesLoanLoss: Double? = null,
    val revenue: Double? = null,
    val revenueCostOther: Double? = null,
    val revenueIncomeInterest: Double? = null,
    val revenueOther: Double? = null,
    val revenueSubsidiaryUnconsolidated: Double? = null,
    val salesCost: Double? = null,
    val sharesIssued: Double? = null,
    val sharesOutstandingPeDateBs: Double? = null,
    val sharesTreasury: Double? = null,
    val stockCommon: Double? = null,
    val stockPreferred: Double? = null,
    val stockPreferredEquity: Double? = null,
    val stockPreferredMezzanine: Double? = null,
    val stockTreasury: Double? = null,
    val symbol: String? = null,
    val wabso: Double? = null,
    val wabsoSplitAdjusted: Double? = null,
    val wadso: Double? = null,
    val wadsoSplitAdjusted: Double? = null,
    val id: String? = null,
    val key: String? = null,
    val subkey: String? = null,
    val date: Long? = null,
    val updated: Long? = null
)


