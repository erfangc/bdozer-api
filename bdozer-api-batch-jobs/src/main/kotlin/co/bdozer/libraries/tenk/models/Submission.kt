package co.bdozer.libraries.tenk.models

data class Submission(
    val addresses: Addresses? = null,
    val category: String? = null,
    val cik: String? = null,
    val description: String? = null,
    val ein: String? = null,
    val entityType: String? = null,
    val exchanges: List<String>? = null,
    val filings: Filings? = null,
    val fiscalYearEnd: String? = null,
    val flags: String? = null,
    val formerNames: List<FormerName>? = null,
    val insiderTransactionForIssuerExists: Int? = null,
    val insiderTransactionForOwnerExists: Int? = null,
    val investorWebsite: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val sic: String? = null,
    val sicDescription: String? = null,
    val stateOfIncorporation: String? = null,
    val stateOfIncorporationDescription: String? = null,
    val tickers: List<String>? = null,
    val website: String? = null
)