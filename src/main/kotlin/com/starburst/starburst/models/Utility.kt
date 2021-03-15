package com.starburst.starburst.models

object Utility {
    const val Period = "Period"

    // Income Statement
    const val Revenue = "Revenue"
    const val CostOfGoodsSold = "CostOfGoodsSold"
    const val GrossProfit = "GrossProfit"
    const val OperatingIncome = "OperatingIncome"
    const val OperatingExpense = "OperatingExpense"
    const val NonOperatingExpense = "NonOperatingExpense"
    const val TaxExpense = "TaxExpense"
    const val PretaxIncome = "PretaxIncome"
    const val InterestExpense = "InterestExpense"
    const val NetIncome = "NetIncome"

    // Balance Sheet
    const val CurrentAsset = "CurrentAsset"
    const val PropertyPlanetAndEquipement = "PropertyPlanetAndEquipement"
    const val LongTermAsset = "LongTermAsset"
    const val TotalAsset = "TotalAsset"
    const val CurrentLiability = "CurrentLiability"
    const val LongTermDebt = "LongTermLiability"
    const val LongTermLiability = "LongTermLiability"
    const val TotalLiability = "TotalLiability"
    const val ShareholdersEquity = "ShareholdersEquity"

    // Non-GaaP / Other
    const val FreeCashFlow = "FreeCashFlow"
    const val FreeCashFlowPerShare = "FreeCashFlowPerShare"
    const val CapitalExpenditure = "CapitalExpenditure"
    const val DepreciationAmortization = "DepreciationAmortization"
    const val StockBasedCompensation = "StockBasedCompensation"
    const val ChangeInWorkingCapital = "ChangeInWorkingCapital"
    const val SharesOutstanding = "SharesOutstanding"
    const val DilutedSharesOutstanding = "DilutedSharesOutstanding"
    const val DiscountFactor = "DiscountFactor"
    const val TerminalValuePerShare = "TerminalValuePerShare"
    const val PresentValuePerShare = "PresentValuePerShare"
    const val EarningsPerShare = "EarningsPerShare"

    fun previous(name: String): String {
        return "Previous_$name"
    }
}
