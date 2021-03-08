package com.starburst.starburst.edgar.dataclasses

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
     * A canonical fact is one in which the context
     * is neither a dimensional nor historical
     *
     * ex: Revenues of FY0, not a historical revenue, not a dimensional revenue
     */
    val canonical: Boolean,

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
