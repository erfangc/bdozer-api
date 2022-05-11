package com.bdozer.api.stockanalysis.master.models

import java.time.LocalDate

data class MT(
    val m_ticker: String?,
    val ticker: String?,
    val comp_name: String?,
    val comp_name_2: String?,
    val exchange: String?,
    val currency_code: String?,
    val ticker_type: String?,
    val active_ticker_flag: String?,
    val comp_url: String?,
    val sic_4_code: Int?,
    val sic_4_desc: String?,
    val zacks_x_ind_code: Int?,
    val zacks_x_ind_desc: String?,
    val zacks_x_sector_code: Int?,
    val zacks_x_sector_desc: String?,
    val zacks_m_ind_code: Int?,
    val zacks_m_ind_desc: String?,
    val per_end_month_nbr: Int?,
    val mr_split_date: LocalDate?,
    val mr_split_factor: Double?,
    val comp_cik: String?,
    val country_code: String?,
    val country_name: String?,
    val comp_type: Int?,
    val optionable_flag: String?,
    val sp500_member_flag: String?,
    val asset_type: String?,
)