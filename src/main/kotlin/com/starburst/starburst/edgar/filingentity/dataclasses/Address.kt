package com.starburst.starburst.edgar.filingentity.dataclasses

data class Address(
    val street1: String? = null,
    val street2: String? = null,
    val city: String? = null,
    val stateOrCountry: String? = null,
    val zipCode: String? = null,
    val stateOrCountryDescription: String? = null,
)