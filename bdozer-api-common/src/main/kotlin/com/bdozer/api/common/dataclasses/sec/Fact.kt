package com.bdozer.api.common.dataclasses.sec

import java.time.LocalDate

/**
 * [Fact] is essentially a normalized version of a fact you find in XBRL
 * filings. It is deduplicated across multiple filings via [FactIdGenerator]
 */
data class Fact(
    val _id: String,
    val instanceDocumentElementId: String,
    val instanceDocumentElementName: String,

    val cik: String,
    val adsh: String,
    val entityName: String,
    val primarySymbol: String,
    val formType: String,

    val conceptName: String,
    val conceptHref: String,
    val namespace: String,

    val instant: LocalDate? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,

    val documentFiscalPeriodFocus: DocumentFiscalPeriodFocus,
    val documentFiscalYearFocus: Int,
    val documentPeriodEndDate: LocalDate,
    val explicitMembers: List<XbrlExplicitMember>,

    val sourceDocument: String,

    val label: String? = null,
    val verboseLabel: String? = null,
    val labelTerse: String? = null,
    val documentation: String? = null,

    val stringValue: String,
    val doubleValue: Double? = null,

    val lastUpdated: String,
)