package com.starburst.starburst.edgar.factbase.filingentity.internal

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Taken from https://data.sec.gov/submissions/CIK{CIK}.json
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SECEntity(
    val cik: String? = null,
    val entityType: String? = null,
    val sic: String? = null,
    val sicDescription: String? = null,
    val insiderTransactionForOwnerExists: Int? = null,
    val insiderTransactionForIssuerExists: Int? = null,
    val name: String? = null,
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

    val addresses: SECEntityAddresses? = null,

    val phone: String? = null,
)