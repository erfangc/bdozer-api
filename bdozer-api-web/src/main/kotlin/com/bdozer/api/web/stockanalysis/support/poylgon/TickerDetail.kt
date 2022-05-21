package com.bdozer.api.web.stockanalysis.support.poylgon

import com.fasterxml.jackson.annotation.JsonProperty


data class TickerDetailV3(
    @JsonProperty("request_id")
    val requestId: String? = null,
    @JsonProperty("results")
    val results: Results? = null,
    @JsonProperty("status")
    val status: String? = null
)

data class Results(
    @JsonProperty("active")
    val active: Boolean? = null,
    @JsonProperty("address")
    val address: Address? = null,
    @JsonProperty("branding")
    val branding: Branding? = null,
    @JsonProperty("cik")
    val cik: String? = null,
    @JsonProperty("composite_figi")
    val compositeFigi: String? = null,
    @JsonProperty("currency_name")
    val currencyName: String? = null,
    @JsonProperty("description")
    val description: String? = null,
    @JsonProperty("homepage_url")
    val homepageUrl: String? = null,
    @JsonProperty("list_date")
    val listDate: String? = null,
    @JsonProperty("locale")
    val locale: String? = null,
    @JsonProperty("market")
    val market: String? = null,
    @JsonProperty("market_cap")
    val marketCap: Double? = null,
    @JsonProperty("name")
    val name: String? = null,
    @JsonProperty("phone_number")
    val phoneNumber: String? = null,
    @JsonProperty("primary_exchange")
    val primaryExchange: String? = null,
    @JsonProperty("share_class_figi")
    val shareClassFigi: String? = null,
    @JsonProperty("share_class_shares_outstanding")
    val shareClassSharesOutstanding: Long? = null,
    @JsonProperty("sic_code")
    val sicCode: String? = null,
    @JsonProperty("sic_description")
    val sicDescription: String? = null,
    @JsonProperty("ticker")
    val ticker: String? = null,
    @JsonProperty("ticker_root")
    val tickerRoot: String? = null,
    @JsonProperty("total_employees")
    val totalEmployees: Int? = null,
    @JsonProperty("type")
    val type: String? = null,
    @JsonProperty("weighted_shares_outstanding")
    val weightedSharesOutstanding: Long? = null
)

data class Address(
    @JsonProperty("address1")
    val address1: String? = null,
    @JsonProperty("city")
    val city: String? = null,
    @JsonProperty("postal_code")
    val postalCode: String? = null,
    @JsonProperty("state")
    val state: String? = null
)

data class Branding(
    @JsonProperty("icon_url")
    val iconUrl: String? = null,
    @JsonProperty("logo_url")
    val logoUrl: String? = null
)