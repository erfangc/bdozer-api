package com.starburst.starburst.zacks

import java.time.LocalDate

data class ZacksFundamentalA(
    val m_ticker: String? = null,
    val ticker: String? = null,
    val comp_name: String? = null,
    val comp_name_2: String? = null,
    val exchange: String? = null,
    val currency_code: String? = null,
    val per_end_date: LocalDate? = null,
    val per_type: String? = null,
    val per_code: String? = null,
    val per_fisc_year: Int? = null,
    val per_fisc_qtr: Int? = null,
    val per_cal_year: Int? = null,
    val per_cal_qtr: Int? = null,
    val data_type_ind: String? = null,
    val filing_type: String? = null,
    val qtr_nbr: Int? = null,
    val zacks_sector_code: Int? = null,
    val zacks_x_ind_code: Int? = null,
    val zacks_metrics_ind_code: Int? = null,
    val fye_month: Int? = null,
    val comp_cik: String? = null,
    val per_len: Int? = null,
    val sic_code: Int? = null,
    val filing_date: LocalDate? = null,
    val last_changed_date: LocalDate? = null,
    val state_incorp_name: String? = null,
    val bus_address_line_1: String? = null,
    val bus_city: String? = null,
    val bus_state_name: String? = null,
    val bus_post_code: String? = null,
    val bus_phone_nbr: String? = null,
    val bus_fax_nbr: String? = null,
    val mail_address_line_1: String? = null,
    val mail_city: String? = null,
    val mail_state_name: String? = null,
    val mail_post_code: String? = null,
    val country_name: String? = null,
    val country_code: String? = null,
    val home_exchange_name: String? = null,
    val emp_cnt: Int? = null,
    val comp_url: String? = null,
    val email_addr: String? = null,
    val nbr_shares_out: Long? = null,
    val shares_out_date: String? = null,
    val officer_name_1: String? = null,
    val officer_title_1: String? = null,
    val officer_name_2: String? = null,
    val officer_title_2: String? = null,
    val officer_name_3: String? = null,
    val officer_title_3: String? = null,
    val officer_name_4: String? = null,
    val officer_title_4: String? = null,
    val officer_name_5: String? = null,
    val officer_title_5: String? = null,
    val tot_revnu: Double? = null,
    val cost_good_sold: Double? = null,
    val gross_profit: Double? = null,
    val tot_oper_exp: Double? = null,
    val oper_income: Double? = null,
    val tot_non_oper_income_exp: Double? = null,
    val pre_tax_income: Double? = null,
    val income_aft_tax: Double? = null,
    val income_cont_oper: Double? = null,
    val consol_net_income_loss: Double? = null,
    val net_income_loss_share_holder: Double? = null,
    val eps_basic_cont_oper: Double? = null,
    val eps_basic_consol: Double? = null,
    val basic_net_eps: Double? = null,
    val eps_diluted_cont_oper: Double? = null,
    val eps_diluted_consol: Double? = null,
    val diluted_net_eps: Double? = null,
    val dilution_factor: Double? = null,
    val avg_d_shares: Double? = null,
    val avg_b_shares: Double? = null,
    val norm_pre_tax_income: Double? = null,
    val norm_aft_tax_income: Double? = null,
    val ebitda: Double? = null,
    val ebit: Double? = null,
    val tot_curr_asset: Double? = null,
    val net_prop_plant_equip: Double? = null,
    val tot_lterm_asset: Double? = null,
    val tot_asset: Double? = null,
    val tot_curr_liab: Double? = null,
    val tot_lterm_debt: Double? = null,
    val tot_lterm_liab: Double? = null,
    val tot_liab: Double? = null,
    val tot_comm_equity: Double? = null,
    val tot_share_holder_equity: Double? = null,
    val tot_liab_share_holder_equity: Double? = null,
    val comm_shares_out: Double? = null,
    val tang_stock_holder_equity: Double? = null,
    val cash_flow_oper_activity: Double? = null,
    val cash_flow_invst_activity: Double? = null,
    val cash_flow_fin_activity: Double? = null,
    val incr_decr_cash: Double? = null,
    val beg_cash: Double? = null,
    val end_cash: Double? = null,
    val stock_based_compsn: Double? = null,
    val comm_stock_div_paid: Double? = null,
    val pref_stock_div_paid: Double? = null,
    val tot_deprec_amort_qd: Double? = null,
    val stock_based_compsn_qd: Double? = null,
    val cash_flow_oper_activity_qd: Double? = null,
    val net_change_prop_plant_equip_qd: Double? = null,
    val comm_stock_div_paid_qd: Double? = null,
    val pref_stock_div_paid_qd: Double? = null,
    val tot_comm_pref_stock_div_qd: Double? = null,
    val wavg_shares_out: Double? = null,
    val wavg_shares_out_diluted: Double? = null,
    val eps_basic_net: Double? = null,
    val eps_diluted_net: Double? = null,
)