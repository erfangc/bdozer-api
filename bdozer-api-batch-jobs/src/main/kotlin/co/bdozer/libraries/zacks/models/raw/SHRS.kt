import co.bdozer.libraries.zacks.models.PrimaryKeyComponent
import java.time.LocalDate

data class SHRS(
    val ticker: String? = null,
    @PrimaryKeyComponent
    val m_ticker: String,
    val comp_name: String? = null,
    val fye: Int? = null,
    @PrimaryKeyComponent
    val per_end_date: LocalDate,
    val per_type: String? = null,
    val active_ticker_flag: String? = null,
    val shares_out: Double? = null,
    val avg_d_shares: Double? = null,
)