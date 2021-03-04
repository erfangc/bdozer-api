package com.starburst.starburst.edgar.dataclasses

import com.starburst.starburst.edgar.dataclasses.XbrlExplicitMember
import com.starburst.starburst.edgar.dataclasses.XbrlPeriod

/**
 * [Fact] is essentially a normalized version of a fact you find in XBRL
 * filings. It is deduplicated across multiple filings via [FactIdGenerator]
 */
data class Fact(
    val _id: String,
    val cik: String,
    val entityName: String,
    val primarySymbol: String,
    val symbols: List<String>,
    val nodeName: String,
    val formType: String,
    // this essentially flattens out
    val period: XbrlPeriod,
    val explicitMembers: List<XbrlExplicitMember>,
    val sourceDocument: String,
    val label: String? = null,
    val verboseLabel: String? = null,
    val labelTerse: String? = null,
    val stringValue: String,
    val doubleValue: Double? = null,
    val lastUpdated: String
)
