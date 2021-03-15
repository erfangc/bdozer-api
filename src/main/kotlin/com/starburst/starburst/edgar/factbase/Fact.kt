package com.starburst.starburst.edgar.factbase

import com.starburst.starburst.edgar.dataclasses.XbrlExplicitMember
import com.starburst.starburst.edgar.dataclasses.XbrlPeriod

/**
 * [Fact] is essentially a normalized version of a fact you find in XBRL
 * filings. It is deduplicated across multiple filings via [FactIdGenerator]
 */
data class Fact(
    val _id: String,
    val instanceDocumentElementId: String,

    val cik: String,
    val entityName: String,
    val primarySymbol: String,
    val symbols: List<String>,
    val formType: String,

    val elementName: String,
    val longNamespace: String,
    val rawElementName: String,

    val period: XbrlPeriod,

    /**
     * This can be Q1, Q2, Q3, Q4
     */
    val documentFiscalPeriodFocus: String,
    val documentFiscalYearFocus: Int,
    val documentPeriodEndDate: String,
    val explicitMembers: List<XbrlExplicitMember>,

    val sourceDocument: String,

    val label: String? = null,
    val verboseLabel: String? = null,
    val labelTerse: String? = null,

    val stringValue: String,
    val doubleValue: Double? = null,

    val lastUpdated: String,
    val adsh: String
)
