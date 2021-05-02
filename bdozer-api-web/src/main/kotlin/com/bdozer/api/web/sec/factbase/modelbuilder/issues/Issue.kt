package com.bdozer.api.web.sec.factbase.modelbuilder.issues

import java.time.Instant

data class Issue(
    val _id: String,
    val stockAnalysisId: String,
    val itemName: String? = null,
    val issueType: IssueType,
    val message: String,
    val createdAt: Instant = Instant.now(),
)