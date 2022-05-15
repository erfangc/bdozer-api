package co.bdozer.libraries.zacks.models.raw

import co.bdozer.libraries.zacks.models.PrimaryKeyComponent

data class MKTV(
    val ticker: String? = null,
    @PrimaryKeyComponent
    val m_ticker: String? = null,
    val comp_name: String? = null,
    val fye: Int? = null,
    @PrimaryKeyComponent
    val per_end_date: String? = null,
    val per_type: String? = null,
    val active_ticker_flag: String? = null,
    val mkt_val: Double? = null,
    val ep_val: Double? = null,
)