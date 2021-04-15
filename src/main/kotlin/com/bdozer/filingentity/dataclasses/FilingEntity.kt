package com.bdozer.filingentity.dataclasses

data class FilingEntity(
    val _id: String,
    val cik: String,
    val tradingSymbol: String? = null,
    val name: String,

    val entityType: String? = null,
    val sic: String? = null,
    val sicDescription: String? = null,
    val insiderTransactionForOwnerExists: Int? = null,
    val insiderTransactionForIssuerExists: Int? = null,
    val tickers: List<String> = emptyList(),
    val exchanges: List<String> = emptyList(),

    val ein: String? = null,
    val description: String? = null,
    val website: String? = null,
    val investorWebsite: String? = null,
    val category: String? = null,
    val fiscalYearEnd: String? = null,
    val stateOfIncorporation: String? = null,
    val stateOfIncorporationDescription: String? = null,

    val phone: String? = null,

    val businessAddress: Address? = null,

    val statusMessage: String? = null,
    val lastUpdated: String,
    val latestAdsh: String? = null,

    val modelTemplate: ModelTemplate? = null,
)

