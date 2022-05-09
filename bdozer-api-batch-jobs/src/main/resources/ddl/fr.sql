create table fr
(
    m_ticker                   varchar,
    ticker                     varchar,
    comp_name                  varchar,
    comp_name_2                varchar,
    exchange                   varchar,
    currency_code              char(3),
    per_end_date               date,
    per_type                   char,
    per_code                   varchar,
    per_fisc_year              int,
    per_fisc_qtr               smallint,
    per_cal_year               int,
    per_cal_qtr                smallint,

    -- indicator columns
    form_7_type                int,
    curr_ratio                 numeric,
    non_perform_asset_tot_loan numeric,
    loan_loss_reserve          numeric,
    lterm_debt_cap             numeric,
    tot_debt_tot_equity        numeric,
    gross_margin               numeric,
    oper_profit_margin         numeric,
    ebit_margin                numeric,
    ebitda_margin              numeric,
    pretax_profit_margin       numeric,
    profit_margin              numeric,
    free_cash_flow             numeric,
    loss_ratio                 numeric,
    exp_ratio                  numeric,
    comb_ratio                 numeric,
    asset_turn                 numeric,
    invty_turn                 numeric,
    rcv_turn                   numeric,
    day_sale_rcv               numeric,
    ret_equity                 numeric,
    ret_tang_equity            numeric,
    ret_asset                  numeric,
    ret_invst                  numeric,
    free_cash_flow_per_share   numeric,
    book_val_per_share         numeric,
    oper_cash_flow_per_share   numeric,

    primary key (m_ticker, per_end_date, per_type)
);
