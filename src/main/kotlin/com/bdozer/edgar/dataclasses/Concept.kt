package com.bdozer.edgar.dataclasses

data class Concept(
    val id: String,
    val targetNamespace: String,
    val conceptHref: String,
    val conceptName: String,
    val type: String? = null,
    val abstract: Boolean? = null,
    val periodType: String? = null,
    val nillable: Boolean? = null,
    val substitutionGroup: String? = null,
    val balance: String? = null,
)
