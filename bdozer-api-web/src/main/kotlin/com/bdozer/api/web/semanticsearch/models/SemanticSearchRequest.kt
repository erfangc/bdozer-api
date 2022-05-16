package com.bdozer.api.web.semanticsearch.models

import com.bdozer.api.web.semanticsearch.models.Document

data class SemanticSearchRequest(val question: String, val documents: List<Document>)