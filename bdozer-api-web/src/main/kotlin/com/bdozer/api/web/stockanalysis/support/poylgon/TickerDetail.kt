package com.bdozer.api.web.stockanalysis.support.poylgon

data class TickerDetail(
    // Indicates if the security is actively listed. If false, this means the company is no longer listed and cannot be traded.
    val active: Boolean? = null,

    // The Bloomberg guid for the symbol.
    val bloomberg: String? = null,

    // The name of the company's current CEO.
    val ceo: String? = null,

    // The official CIK guid used for SEC database/filings.
    val cik: String? = null,

    // The country in which the company is registered.
    val country: String? = null,

    // A description of the company and what they do/offer.
    val description: String? = null,

    // The approximate number of employees for the company.,
    val employees: Int? = null,

    // The symbol's primary exchange.
    val exchange: String? = null,

    // The exchange code (id) of the symbol's primary exchange.
    val exchangeSymbol: String? = null,

    // The OpenFigi project guid for the symbol. (https://openfigi.com/)
    val figi: String? = null,

    // The street address for the company's headquarters.
    val hq_address: String? = null,

    // The country in which the company's headquarters is located.
    val hq_country: String? = null,

    // The state in which the company's headquarters is located.
    val hq_state: String? = null,

    // The industry in which the company operates.
    val industry: String? = null,

    // The Legal Entity Identifier (LEI) guid for the symbol. (https://en.wikipedia.org/wiki/Legal_Entity_Identifier)
    val lei: String? = null,

    // The date that the symbol was listed on the exchange.
    val listdate: String? = null,

    // The URL of the entity's logo.
    val logo: String? = null,

    // The current market cap for the company.
    val marketcap: Double? = null,

    // The name of the company/entity.
    val name: String? = null,

    // The phone number for the company. This is usually a corporate contact number.
    val phone: String? = null,

    // The sector of the indsutry in which the symbol operates.
    val sector: String? = null,

    // Standard Industrial Classification (SIC) id for the symbol. (https://en.wikipedia.org/wiki/Legal_Entity_Identifier)
    val sic: Int? = null,

    // A list of ticker symbols for similar companies.
    val similar: List<String>? = null,

    // The exchange symbol that this item is traded under.
    val symbol: String? = null,

    // A list of words related to the company.
    val tags: List<String>? = null,

    // The type or class of the security. (Full List of Ticker Types)
    val type: String? = null,

    // The last time this company record was updated.
    val updated: String? = null,

    // The URL of the company's website
    val url: String? = null,
)