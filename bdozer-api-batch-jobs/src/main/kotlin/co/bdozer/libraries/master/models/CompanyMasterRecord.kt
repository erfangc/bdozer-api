package co.bdozer.libraries.master.models

data class CompanyMasterRecord(
    val id: String,
    val ticker: String,
    val cik: String?,
    val exchange: String?,
    val companyUrl: String?,
    val companyName: String?,
    val price: Double?,
    val marketCap: Double?,
    val enterpriseValue: Double?,
    val perShareMetrics: PerShareMetrics?,
    val latestMetrics: LatestMetrics?,
    val sales: Trend?,
    val earnings: Trend?,
)
