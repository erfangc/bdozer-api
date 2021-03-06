package com.bdozer.api.web.stockanalysis.support.zacks.models

import java.time.LocalDate

data class FC(
    val m_ticker: String,
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
    val qtr_nbr: String? = null,
    val zacks_sector_code: String? = null,
    val zacks_x_ind_code: String? = null,
    val zacks_metrics_ind_code: String? = null,
    val fye_month: String? = null,
    val comp_cik: String? = null,
    val sic_code: String? = null,
    val filing_date: String? = null,
    val last_changed_date: String? = null,
    val state_incorp_name: String? = null,
    val emp_cnt: String? = null,
    val emp_pt_cnt: String? = null,
    val emp_ft_cnt: String? = null,
    val emp_other_cnt: String? = null,
    val comm_share_holder: String? = null,
    val auditor: String? = null,
    val auditor_opinion: String? = null,
    val comp_url: String? = null,
    val email_addr: String? = null,
    val nbr_shares_out: String? = null,
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
    val rpt_0_date: String? = null,
    val rpt_1_date: String? = null,
    val rpt_2_date: String? = null,
    val wavg_shares_out_diluted: Double? = null,
    val wavg_shares_out: Double? = null,
    val avg_b_shares: Double? = null,
    val avg_d_shares: Double? = null,
    val treas_stock: Double? = null,
    val tot_share_holder_equity: Double? = null,
    val tot_sell_gen_admin_exp: Double? = null,
    val tot_revnu: Double? = null,
    val tot_provsn_income_tax: Double? = null,
    val tot_pref_stock: Double? = null,
    val tot_oper_exp: Double? = null,
    val tot_non_oper_income_exp: Double? = null,
    val tot_non_cash_item: Double? = null,
    val tot_lterm_liab: Double? = null,
    val tot_lterm_debt: Double? = null,
    val tot_lterm_asset: Double? = null,
    val tot_liab_share_holder_equity: Double? = null,
    val tot_liab: Double? = null,
    val tot_deprec_amort_qd: Double? = null,
    val tot_deprec_amort_cash_flow: Double? = null,
    val tot_deprec_amort: Double? = null,
    val tot_curr_liab: Double? = null,
    val tot_curr_asset: Double? = null,
    val tot_comm_pref_stock_div_qd: Double? = null,
    val tot_comm_pref_stock_div_paid: Double? = null,
    val tot_comm_equity: Double? = null,
    val tot_change_asset_liab: Double? = null,
    val tot_asset: Double? = null,
    val tot_accum_deprec: Double? = null,
    val tang_stock_holder_equity: Double? = null,
    val stock_div_subsid: Double? = null,
    val stock_based_compsn_qd: Double? = null,
    val stock_based_compsn: Double? = null,
    val spcl_unusual_charge: Double? = null,
    val retain_earn_accum_deficit: Double? = null,
    val res_dev_exp: Double? = null,
    val restruct_charge: Double? = null,
    val rental_income: Double? = null,
    val rental_exp_ind_broker: Double? = null,
    val rcv_tot: Double? = null,
    val rcv_est_doubt: Double? = null,
    val pre_tax_minority_int: Double? = null,
    val pre_tax_income: Double? = null,
    val prepaid_expense: Double? = null,
    val pref_stock_shares_out: Double? = null,
    val pref_stock_liab: Double? = null,
    val pref_stock_div_paid_qd: Double? = null,
    val pref_stock_div_paid: Double? = null,
    val pref_stock_div_other_adj: Double? = null,
    val pension_post_retire_liab: Double? = null,
    val pension_post_retire_exp: Double? = null,
    val pension_post_retire_asset: Double? = null,
    val other_share_holder_equity: Double? = null,
    val other_pay: Double? = null,
    val other_oper_income_exp: Double? = null,
    val other_non_oper_income_exp: Double? = null,
    val other_non_curr_liab: Double? = null,
    val other_non_cash_item: Double? = null,
    val other_lterm_asset: Double? = null,
    val other_income: Double? = null,
    val other_curr_liab: Double? = null,
    val other_curr_asset: Double? = null,
    val other_accrued_exp: Double? = null,
    val oper_income: Double? = null,
    val oper_activity_other: Double? = null,
    val note_pay: Double? = null,
    val note_loan_rcv: Double? = null,
    val norm_pre_tax_income: Double? = null,
    val norm_aft_tax_income: Double? = null,
    val non_oper_int_exp: Double? = null,
    val non_ctl_int: Double? = null,
    val net_tot_equity_issued_repurch: Double? = null,
    val net_real_estate_misc_prop: Double? = null,
    val net_prop_plant_equip: Double? = null,
    val net_pref_equity_issued_repurch: Double? = null,
    val net_lterm_debt: Double? = null,
    val net_income_parent_comp: Double? = null,
    val net_income_loss_share_holder: Double? = null,
    val net_income_loss: Double? = null,
    val net_curr_debt: Double? = null,
    val net_comm_equity_issued_repurch: Double? = null,
    val net_change_sterm_invst: Double? = null,
    val net_change_prop_plant_equip_qd: Double? = null,
    val net_change_prop_plant_equip: Double? = null,
    val net_change_lterm_invst: Double? = null,
    val net_change_invst_tot: Double? = null,
    val net_change_intang_asset: Double? = null,
    val net_acq_divst: Double? = null,
    val min_int: Double? = null,
    val minority_int: Double? = null,
    val merger_acq_income_aggr: Double? = null,
    val mand_redeem_pref_sec_subsid: Double? = null,
    val lterm_rcv: Double? = null,
    val lterm_invst: Double? = null,
    val litig_aggr: Double? = null,
    val liab_disc_oper_lterm: Double? = null,
    val liab_discont_oper_curr: Double? = null,
    val in_proc_res_dev_exp_aggr: Double? = null,
    val invty_lterm: Double? = null,
    val invty: Double? = null,
    val invst_gain_loss_other: Double? = null,
    val invst_activity_other: Double? = null,
    val int_invst_income_oper: Double? = null,
    val int_invst_income: Double? = null,
    val int_exp_oper: Double? = null,
    val int_cap: Double? = null,
    val incr_decr_cash: Double? = null,
    val income_loss_equity_invst_other: Double? = null,
    val income_discont_oper: Double? = null,
    val income_cont_oper: Double? = null,
    val income_bef_exord_acct_change: Double? = null,
    val income_aft_tax: Double? = null,
    val impair_goodwill: Double? = null,
    val gross_prop_plant_equip: Double? = null,
    val gross_profit: Double? = null,
    val goodwill_intang_asset_tot: Double? = null,
    val gain_loss_sale_invst_aggr: Double? = null,
    val gain_loss_sale_asset_aggr: Double? = null,
    val fin_activity_other: Double? = null,
    val fgn_exchange_rate_adj: Double? = null,
    val exord_income_loss: Double? = null,
    val equity_equiv: Double? = null,
    val equity_earn_subsid: Double? = null,
    val eps_diluted_parent_comp: Double? = null,
    val eps_diluted_net: Double? = null,
    val eps_diluted_extra: Double? = null,
    val eps_diluted_discont_oper: Double? = null,
    val eps_diluted_cont_oper: Double? = null,
    val eps_diluted_consol: Double? = null,
    val eps_diluted_acct_change: Double? = null,
    val eps_basic_parent_comp: Double? = null,
    val eps_basic_net: Double? = null,
    val eps_basic_extra: Double? = null,
    val eps_basic_discont_oper: Double? = null,
    val eps_basic_cont_oper: Double? = null,
    val eps_basic_consol: Double? = null,
    val eps_basic_acct_change: Double? = null,
    val end_cash: Double? = null,
    val ebitda: Double? = null,
    val ebit: Double? = null,
    val div_pay: Double? = null,
    val disc_oper_misc_cash_flow_adj: Double? = null,
    val dilution_factor: Double? = null,
    val diluted_net_eps: Double? = null,
    val def_tax_asset_lterm: Double? = null,
    val def_tax_asset_curr: Double? = null,
    val def_compsn: Double? = null,
    val def_charge_non_curr: Double? = null,
    val def_charge_curr: Double? = null,
    val defer_tax_liab_lterm: Double? = null,
    val defer_tax_liab_curr: Double? = null,
    val defer_revnu_non_curr: Double? = null,
    val defer_revnu_curr: Double? = null,
    val debt_issue_retire_net_tot: Double? = null,
    val curr_portion_tax_pay: Double? = null,
    val curr_portion_debt: Double? = null,
    val curr_portion_cap_lease: Double? = null,
    val cumul_eff_acct_change: Double? = null,
    val cost_good_sold: Double? = null,
    val consol_net_income_loss: Double? = null,
    val compr_income: Double? = null,
    val comm_stock_net: Double? = null,
    val comm_stock_div_paid_qd: Double? = null,
    val comm_stock_div_paid: Double? = null,
    val comm_shares_out: Double? = null,
    val change_invty: Double? = null,
    val change_income_tax: Double? = null,
    val change_asset_liab: Double? = null,
    val change_acct_rcv: Double? = null,
    val change_acct_pay_accrued_liab: Double? = null,
    val change_acct_pay: Double? = null,
    val cash_sterm_invst: Double? = null,
    val cash_flow_oper_activity_qd: Double? = null,
    val cash_flow_oper_activity: Double? = null,
    val cash_flow_invst_activity: Double? = null,
    val cash_flow_fin_activity: Double? = null,
    val cap_software: Double? = null,
    val beg_cash: Double? = null,
    val basic_net_eps: Double? = null,
    val asset_wdown_impair_aggr: Double? = null,
    val asset_discont_oper_lterm: Double? = null,
    val asset_discont_oper_curr: Double? = null,
    val adv_dep: Double? = null,
    val addtl_paid_in_cap: Double? = null,
    val acct_pay: Double? = null,
    val accrued_exp: Double? = null,
)