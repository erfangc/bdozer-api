package com.bdozer.api.web.semanticsearch.models

data class Document(val id: String, val text: String, val metadata: Map<String, Any>)