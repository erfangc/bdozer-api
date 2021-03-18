package com.starburst.starburst.zacks.dataclasses

import com.starburst.starburst.models.evaluator.EvaluateModelResult

data class BuildModelResponse(
    val evaluateModelResult: EvaluateModelResult,
    val incomeStatement: IncomeStatement,
    val balanceSheet: BalanceSheet
)