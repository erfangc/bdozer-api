package co.bdozer.libraries.polygon.models

data class Results(
    val active: Boolean,
    val address: Address,
    val branding: Branding? = null,
    val cik: String,
    val composite_figi: String? = null,
    val currency_name: String? = null,
    val description: String? = null,
    val homepage_url: String? = null,
    val list_date: String? = null,
    val locale: String? = null,
    val market: String? = null,
    val market_cap: Double,
    val name: String? = null,
    val phone_number: String? = null,
    val primary_exchange: String? = null,
    val share_class_figi: String? = null,
    val share_class_shares_outstanding: Double,
    val sic_code: String? = null,
    val sic_description: String? = null,
    val ticker: String,
    val total_employees: Int? = null,
    val type: String? = null,
    val weighted_shares_outstanding: Double? = null
)