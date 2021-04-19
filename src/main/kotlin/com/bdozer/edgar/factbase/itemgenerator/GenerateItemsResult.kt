package com.bdozer.edgar.factbase.itemgenerator

import com.bdozer.models.dataclasses.Item

data class GenerateItemsResult(
    val incomeStatementItems: List<Item> = emptyList(),
    val balanceSheetItems: List<Item> = emptyList(),

    val revenue: Item? = null,
    val netIncome: Item? = null,

    val epsBasic: Item? = null,
    val epsDiluted: Item? = null,
    val epsBasicAndDiluted: Item? = null,

    val basicSharesOutstanding: Item? = null,
    val dilutedSharesOutstanding: Item? = null,
    val basicAndDilutedSharesOutstanding: Item? = null,
)